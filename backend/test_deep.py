import urllib.request
import re
import feedparser

feed = feedparser.parse('https://www.dealdoktor.de/feed/')
for e in feed.entries[:3]:
    url = e.link
    print(f"Checking {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        html = urllib.request.urlopen(req, timeout=5).read().decode('utf-8')
        # Find ASIN in the whole HTML
        asin_match = re.search(r'(?i)(?:/dp/|/gp/product/|/product/|\bASIN=|\basin=|\bASIN\.1=)([B][A-Z0-9]{9})\b', html)
        if asin_match:
            print(f"Found ASIN: {asin_match.group(1)}")
        else:
            print("No ASIN found.")
    except Exception as ex:
        print(f"Error: {ex}")
