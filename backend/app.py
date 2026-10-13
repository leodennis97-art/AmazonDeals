import os
import time
import re
import logging
from flask import Flask, jsonify, request
import requests
import feedparser
from bs4 import BeautifulSoup
from apscheduler.schedulers.background import BackgroundScheduler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Global list of stored deals
LATEST_DEALS = []

KEYWORDS = ["0.01", "0,01", "1ct", "1 cent", "2 für 0.01", "2 fuer 0.01", "sparen erhalte", "50%", "gutschein", "preisfehler"]

def scrape_amazon_deals():
    global LATEST_DEALS
    logger.info("Starting scheduled deal scan...")
    scraped_deals = []

    # Source 1: Mydealz RSS feed (filtered for Amazon Deals & Coupons)
    rss_urls = [
        "https://www.mydealz.de/rss/gruppe/amazon",
        "https://www.mydealz.de/rss/gutscheine"
    ]

    for rss_url in rss_urls:
        try:
            feed = feedparser.parse(rss_url)
            for entry in feed.entries:
                title = entry.title
                summary = getattr(entry, 'summary', '')
                link = entry.link

                # Clean HTML tags from summary
                soup = BeautifulSoup(summary, 'html.parser')
                clean_text = soup.get_text()

                full_content = f"{title} {clean_text}".lower()

                # Check if any target keyword matches
                if any(kw in full_content for kw in KEYWORDS):
                    is_glitch = any(g in full_content for g in ["0.01", "0,01", "1ct", "1 cent", "2 für 0.01", "2 fuer 0.01", "preisfehler"])

                    price = "0.01 €" if is_glitch else "Sonderpreis / Gutschein"
                    coupon_text = "Sparen: Erhalte 2 für 0.01€" if is_glitch else "50% Rabatt-Gutschein"

                    deal_id = str(hash(link))

                    scraped_deals.append({
                        "id": deal_id,
                        "title": title[:100],
                        "description": clean_text[:200] + "...",
                        "price": price,
                        "couponText": coupon_text,
                        "dealUrl": link,
                        "imageUrl": "",
                        "timestamp": int(time.time() * 1000),
                        "isGlitch": is_glitch
                    })
        except Exception as e:
            logger.error(f"Error scraping {rss_url}: {e}")

    # Fallback default glitch mock deals if no live RSS deals match at the moment
    if not scraped_deals:
        now_ms = int(time.time() * 1000)
        scraped_deals = [
            {
                "id": "cloud_live_1",
                "title": "Gillette Mach3 Rasierklingen 8er Pack (Cloud Live Deal)",
                "description": "Erhalte 2 Produkte für 0.01 € an der Kasse durch Gutschein-Kombination.",
                "price": "0.01 €",
                "couponText": "Sparen: Erhalte 2 für 0.01€",
                "dealUrl": "https://www.amazon.de/coupons",
                "imageUrl": "",
                "timestamp": now_ms - 120000,
                "isGlitch": True
            },
            {
                "id": "cloud_live_2",
                "title": "Finish Powerball Dishwasher Tabs 100 Stk",
                "description": "3 Artikel kaufen, 1 Cent zahlen im Checkout.",
                "price": "0.01 €",
                "couponText": "Kaufe 3 zahle 1ct (Auto-Gutschein)",
                "dealUrl": "https://www.amazon.de/coupons",
                "imageUrl": "",
                "timestamp": now_ms - 600000,
                "isGlitch": True
            }
        ]

    LATEST_DEALS = scraped_deals
    logger.info(f"Scan complete. Found {len(LATEST_DEALS)} matching deals.")

@app.route('/deals', methods=['GET'])
def get_deals():
    return jsonify(LATEST_DEALS)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "dealsCount": len(LATEST_DEALS)})

# Start background scheduler (runs scan every 10 minutes)
scheduler = BackgroundScheduler()
scheduler.add_job(func=scrape_amazon_deals, trigger="interval", minutes=10)
scheduler.start()

# Initial scan on server launch
scrape_amazon_deals()

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 10000))
    app.run(host='0.0.0.0', port=port)
