import urllib.request
import re

req = urllib.request.Request('https://www.dealdoktor.de/2kg-by-amazon-intenso-kaffeebohnen/', headers={'User-Agent': 'Mozilla/5.0'})
html = urllib.request.urlopen(req).read().decode('utf-8')

links = set(re.findall(r'https?://[^\s\"\'<>]+', html))
amazon_links = [l for l in links if 'amazon' in l.lower() or 'amzn' in l.lower()]
print(amazon_links)
