import os
import sys
import threading
import time

try:
    from pyngrok import ngrok
except ImportError:
    print("Installing pyngrok...")
    os.system(f"{sys.executable} -m pip install pyngrok")
    from pyngrok import ngrok

from app import app, scrape_amazon_deals

PORT = 5000

def start_flask():
    app.run(host='0.0.0.0', port=PORT, use_reloader=False)

if __name__ == '__main__':
    print("==================================================")
    print("🚀 Starting AmazonDeals Local Scraper & ngrok Tunnel")
    print("==================================================")

    # Start Flask server in background thread
    flask_thread = threading.Thread(target=start_flask, daemon=True)
    flask_thread.start()

    time.sleep(2) # Give Flask a moment to start

    try:
        # Open an HTTP tunnel on port 5000
        public_url = ngrok.connect(PORT).public_url
        print("\n✅ SUCCESS! ngrok Tunnel is live:")
        print(f"👉 NGROK URL: {public_url}")
        print("\n📲 Trage diese URL in der Android App in den Einstellungen ein:")
        print(f"   {public_url}")
        print("\nDie App stellt ab sofort eine Verbindung zu deinem lokalen Scraper her!\n")
        print("Drücke STRG+C zum Beenden.")

        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nBeende ngrok und Server...")
        ngrok.kill()
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Fehler beim Starten von ngrok: {e}")
        print("\nAlternativ kannst du ngrok manuell im Terminal starten:")
        print("1) python app.py")
        print("2) ngrok http 5000")
