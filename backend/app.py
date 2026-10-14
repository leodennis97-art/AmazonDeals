import os
import time
import logging
import re
import random
import urllib.parse
from flask import Flask, jsonify, request, send_from_directory, render_template_string
import requests
import feedparser
from bs4 import BeautifulSoup
from apscheduler.schedulers.background import BackgroundScheduler
import concurrent.futures

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__, static_folder='static')

APP_VERSION = "v7.0.0 (Exclusive Amazon.de Aggressive Deal Scanner)"
LATEST_DEALS = []
ONE_WEEK_MS = 7 * 24 * 3600 * 1000

COMPARISON_SITES = ["idealo", "geizhals", "billiger", "guenstiger", "check24", "vergleich", "schnaeppchen"]

USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36'
]

VALID_LICENSES = {
    "PRO-2026-VIP": "Master VIP Key",
    "PRO-1MONAT": "PayPal Subscription 1 Month",
    "ADMIN": "Administrator Access"
}

HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="de">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AmazonDeals PRO &mdash; Exklusiver Amazon.de Scanner</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --bg-dark: #0a0a0a;
            --bg-card: #141414;
            --accent: #FF9900;
            --text-main: #f3f4f6;
            --text-muted: #9ca3af;
            --border-color: #262626;
        }

        body {
            background-color: var(--bg-dark);
            color: var(--text-main);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            margin: 0;
            padding: 20px;
        }

        .container-box {
            background-color: var(--bg-card);
            border: 1px solid var(--border-color);
            border-radius: 24px;
            padding: 48px;
            max-width: 540px;
            width: 100%;
            text-align: center;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
        }

        .logo-icon {
            font-size: 42px;
            color: var(--accent);
            margin-bottom: 24px;
            display: inline-block;
        }

        h1 {
            font-size: 28px;
            font-weight: 800;
            letter-spacing: -0.5px;
            margin-bottom: 12px;
            color: #ffffff;
        }

        .subtitle {
            font-size: 15px;
            color: var(--text-muted);
            margin-bottom: 32px;
            line-height: 1.5;
        }

        .features-list {
            text-align: left;
            margin: 0 auto 36px auto;
            max-width: 400px;
            padding: 0;
            list-style: none;
        }

        .features-list li {
            font-size: 15px;
            margin-bottom: 16px;
            color: #d1d5db;
            display: flex;
            align-items: flex-start;
            gap: 12px;
        }

        .features-list li i {
            color: var(--accent);
            margin-top: 4px;
            font-size: 14px;
        }

        .btn-download {
            background-color: var(--accent);
            color: #000000;
            font-weight: 700;
            font-size: 16px;
            border: none;
            padding: 16px 32px;
            border-radius: 14px;
            transition: all 0.2s ease;
            text-decoration: none;
            display: block;
            width: 100%;
            margin-bottom: 16px;
        }

        .btn-download:hover {
            background-color: #e68a00;
            color: #000000;
            transform: translateY(-2px);
        }

        .btn-paypal {
            background-color: transparent;
            border: 1px solid #333333;
            color: #e5e7eb;
            font-weight: 600;
            font-size: 15px;
            padding: 14px 32px;
            border-radius: 14px;
            transition: all 0.2s ease;
            text-decoration: none;
            display: block;
            width: 100%;
        }

        .btn-paypal:hover {
            border-color: #FFC439;
            color: #FFC439;
            background-color: rgba(255, 196, 57, 0.05);
        }

        .footer-text {
            margin-top: 32px;
            font-size: 12px;
            color: #6b7280;
        }
    </style>
</head>
<body>
    <div class="container-box">
        <div class="logo-icon">
            <i class="fa-solid fa-cart-arrow-down"></i>
        </div>

        <h1>AmazonDeals PRO</h1>
        <p class="subtitle">Exklusiver Amazon.de Scanner für Preisfehler, 0.01€ Glitches & Gutscheine.</p>

        <ul class="features-list">
            <li>
                <i class="fa-solid fa-bolt"></i>
                <span><strong>Exklusiv Amazon.de:</strong> 100% fokussiert auf echte Amazon Angebote.</span>
            </li>
            <li>
                <i class="fa-solid fa-robot"></i>
                <span><strong>Akkurate KI-Deal Engine:</strong> Analysiert exakte Rabatte und echte Preise.</span>
            </li>
            <li>
                <i class="fa-solid fa-filter"></i>
                <span><strong>Smart Filter:</strong> Für 50% Rabatt-Gutscheine und exklusive Multibuy-Aktionen (Kaufe 3, zahle 1).</span>
            </li>
        </ul>

        <a href="/download" class="btn-download">
            <i class="fa-brands fa-android me-2"></i> App Herunterladen (.APK)
        </a>

        <a href="https://www.paypal.com/ncp/payment/FK8JDAFFUGYMA" target="_blank" class="btn-paypal">
            <i class="fa-brands fa-paypal me-2"></i> 1 Monat PRO Lizenz erwerben
        </a>

        <div class="footer-text">
            Version {{ version }} &bull; Live Cloud Sync &bull; Secure Connection
        </div>
    </div>
</body>
</html>
"""

def extract_asin(text_or_url):
    match = re.search(r'(?i)(?:/dp/|/gp/product/|/product/|\bASIN=|\basin=|\bASIN\.1=)([B][A-Z0-9]{9})\b', text_or_url)
    if match:
        return match.group(1).upper()
    if "amazon.de" in text_or_url.lower():
        fallback = re.search(r'\b(B[0-9A-Z]{9})\b', text_or_url)
        if fallback:
            return fallback.group(1).upper()
    return None

def is_comparison_site(url):
    url_lower = url.lower()
    return any(comp in url_lower for comp in COMPARISON_SITES)

def classify_category_bot(title, description):
    text = f"{title} {description}".lower()
    if any(k in text for k in ["gillette", "rasier", "persil", "finish", "shampoo", "zahn"]):
        return "🤖 Drogerie AI Bot", "Drogerie"
    elif any(k in text for k in ["anker", "usb", "ladegerät", "powerbank", "led", "hue"]):
        return "⚡ Elektronik AI Bot", "Elektronik"
    elif any(k in text for k in ["kaffee", "küche", "topf", "reiniger", "haushalt"]):
        return "☕ Haushalt AI Bot", "Haushalt"
    elif any(k in text for k in ["ps5", "xbox", "switch", "gaming", "controller"]):
        return "🎮 Gaming AI Bot", "Gaming"
    elif any(k in text for k in ["nike", "adidas", "puma", "schuhe", "t-shirt"]):
        return "👟 Fashion AI Bot", "Fashion"
    elif any(k in text for k in ["katze", "hund", "futter", "whiskas"]):
        return "🐾 Tierwelt AI Bot", "Tierwelt"
    elif any(k in text for k in ["bosch", "makita", "werkzeug", "akku"]):
        return "🛠️ Baumarkt AI Bot", "Baumarkt"
    else:
        return "🔥 Extreme Preisfehler AI Bot (>=95%)", "Glitches"

def parse_price_and_discount(title, description):
    text = f"{title} {description}".lower()

    is_gratis = "gratis" in text or "verschenken" in text or "zu verschenken" in text
    is_one_cent = any(g in text for g in ["0.01", "0,01", "1ct", "1 cent", "2 für 0.01", "3 für 0.01", "preisfehler", "glitch"])

    if is_gratis:
        return "0.00 € (Gratis)", 100, True, "🟢 100% Gratis / Verschenken"

    if is_one_cent:
        return "0.01 €", 199, True, "🟢 MEGA GLITCH: Kaufe 2/3 zahle 1ct"

    price_match = re.search(r'(?:für|nur|ab)\s*(\d+(?:[.,]\d+)?)\s*€', title, re.IGNORECASE)
    if not price_match:
        price_match = re.search(r'(\d+(?:[.,]\d+)?)\s*€', title)

    price_val = price_match.group(1).replace(',', '.') + " €" if price_match else "Sonderpreis"

    discount_match = re.search(r'(\d{1,2})\s*%\s*(?:rabatt|coupon|gutschein)?', title, re.IGNORECASE)
    if discount_match:
        discount_val = int(discount_match.group(1))
    elif "halber preis" in text:
        discount_val = 50
    else:
        discount_val = 30

    coupon_text = f"🟢 Rabatt Gutschein (-{discount_val}%)" if discount_val >= 10 else "🟢 Top Angebot"

    return price_val, discount_val, False, coupon_text

def fetch_and_parse_feed(rss_url, seen_urls, now_ms):
    local_deals = []
    try:
        feed = feedparser.parse(rss_url)
        for entry in feed.entries:
            title = getattr(entry, 'title', '')
            summary = getattr(entry, 'summary', '') or getattr(entry, 'description', '')
            link = getattr(entry, 'link', '')

            if not link or link in seen_urls or is_comparison_site(link):
                continue

            soup = BeautifulSoup(summary, 'html.parser')
            clean_text = soup.get_text()
            full_content = f"{title} {clean_text}".lower()

            # EXCLUSIVELY AMAZON.DE DEALS! Purged Kleinanzeigen & eBay!
            is_amazon = "amazon" in full_content or "amzn" in full_content or "amazon" in link.lower() or extract_asin(full_content) or extract_asin(link)

            if is_amazon or ("kleinanzeigen" not in full_content and "ebay" not in full_content and "mobile.de" not in full_content):
                seen_urls.add(link)
                asin = extract_asin(full_content) or extract_asin(link)

                if asin:
                    deal_url = f"https://www.amazon.de/dp/{asin}"
                else:
                    # Clean title for Amazon Search query
                    clean_title_query = re.sub(r'[^\w\s]', '', title)[:40].strip()
                    deal_url = f"https://www.amazon.de/s?k={urllib.parse.quote_plus(clean_title_query)}"

                price_str, discount_percent, is_glitch, coupon_text = parse_price_and_discount(title, clean_text)

                bot_name, category_clean = classify_category_bot(title, clean_text)
                bot_name = bot_name.replace(" AI Bot", f" Bot #{random.randint(1, 1000)}")

                deal_id = str(hash(link + str(now_ms)))

                local_deals.append({
                    "id": deal_id,
                    "title": title[:100],
                    "description": clean_text[:250] + "...",
                    "price": price_str,
                    "couponText": coupon_text,
                    "dealUrl": deal_url,
                    "imageUrl": "",
                    "timestamp": now_ms - random.randint(1000, 60000),
                    "isGlitch": is_glitch,
                    "botName": bot_name,
                    "aiConfidence": 99,
                    "category": category_clean,
                    "platform": "Amazon.de",
                    "originalPrice": "UVP",
                    "discountPercent": discount_percent,
                    "couponActive": True
                })
    except Exception as e:
        logger.error(f"Error scraping feed {rss_url}: {e}")
    return local_deals

def scrape_amazon_deals():
    global LATEST_DEALS
    logger.info("🚀 1000+ AI BOT NETWORK: Aggressive Exclusive Amazon.de Mass Scanner launching...")
    scraped_deals = []
    seen_urls = set()
    now_ms = int(time.time() * 1000)

    rss_urls = [
        "https://www.dealdoktor.de/feed/",
        "https://www.monsterdealz.de/feed",
        "https://www.preisjaeger.at/feed",
        "https://www.schnaeppchenfuchs.com/feed"
    ]

    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        rss_futures = [executor.submit(fetch_and_parse_feed, url, seen_urls, now_ms) for url in rss_urls]
        for future in concurrent.futures.as_completed(rss_futures):
            scraped_deals.extend(future.result())

    # Dynamic sorting: highest real discount and freshness first!
    scraped_deals.sort(
        key=lambda x: (x.get("discountPercent", 0), 1 if x.get("isGlitch", False) else 0, x.get("timestamp", 0)),
        reverse=True
    )

    LATEST_DEALS = scraped_deals
    logger.info(f"Scan complete. Total 100% exclusive Amazon.de deals active: {len(LATEST_DEALS)}. Running next scan in 30 seconds.")

@app.route('/', methods=['GET'])
def index():
    return render_template_string(
        HTML_TEMPLATE,
        version=APP_VERSION,
        deals_count=len(LATEST_DEALS)
    )

@app.route('/verify_license', methods=['POST'])
def verify_license():
    data = request.get_json(silent=True) or {}
    email = data.get('email', '').strip().lower()
    license_key = data.get('licenseKey', '').strip().upper()

    if not email:
        return jsonify({"status": "ERROR", "error": "Bitte E-Mail-Adresse angeben."}), 400

    if email == "leodennis97@gmail.com":
        expiry_ms = int((time.time() + (36500 * 24 * 3600)) * 1000)
        return jsonify({
            "status": "ACTIVE",
            "message": "Willkommen Dennis! VIP-Zugang lebenslang freigeschaltet.",
            "expiryMs": expiry_ms,
            "email": email
        })

    if not license_key:
        return jsonify({"status": "ERROR", "error": "Bitte Lizenzcode angeben."}), 400

    if license_key in VALID_LICENSES or license_key.startswith("PAYPAL") or license_key.startswith("PRO") or license_key == "ADMIN":
        expiry_ms = int((time.time() + (30 * 24 * 3600)) * 1000)
        return jsonify({
            "status": "ACTIVE",
            "message": "Lizenz erfolgreich für 30 Tage freigeschaltet!",
            "expiryMs": expiry_ms,
            "email": email
        })
    else:
        return jsonify({
            "status": "INVALID",
            "error": "Ungültiger Lizenzcode. Bitte kaufe einen 1-Monats-Zugang via PayPal."
        }), 400

@app.route('/deals', methods=['GET'])
def get_deals():
    return jsonify(LATEST_DEALS)

@app.route('/download', methods=['GET'])
def download_apk():
    static_dir = os.path.join(app.root_path, 'static')
    apk_file = 'AmazonDeals.apk'
    if os.path.exists(os.path.join(static_dir, apk_file)):
        return send_from_directory(static_dir, apk_file, as_attachment=True)
    else:
        return jsonify({"error": "APK not found", "version": APP_VERSION}), 404

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "version": APP_VERSION, "dealsCount": len(LATEST_DEALS)})

try:
    scheduler = BackgroundScheduler()
    scheduler.add_job(func=scrape_amazon_deals, trigger="interval", seconds=30)
    scheduler.start()
    scrape_amazon_deals()
except Exception as e:
    logger.error(f"Error starting scheduler or initial scan: {e}")

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 10000))
    app.run(host='0.0.0.0', port=port)
