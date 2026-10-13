import os
import sys
import threading
import time

try:
    from pyngrok import ngrok, conf
except ImportError:
    print("Installing pyngrok...")
    os.system(f"{sys.executable} -m pip install pyngrok")
    from pyngrok import ngrok, conf

from app import app, scrape_amazon_deals

PORT = 5000
AUTHTOKEN_FILE = "backend/ngrok_authtoken.txt"

def get_authtoken():
    if os.path.exists(AUTHTOKEN_FILE):
        with open(AUTHTOKEN_FILE, "r", encoding="utf-8") as f:
            token = f.read().strip()
            if token:
                return token

    env_token = os.environ.get("NGROK_AUTHTOKEN", "").strip()
    if env_token:
        return env_token

    return None

def start_flask():
    app.run(host='0.0.0.0', port=PORT, use_reloader=False)

if __name__ == '__main__':
    print("==================================================")
    print("🚀 AmazonDeals Local Scraper & ngrok Tunnel Launcher")
    print("==================================================")

    token = get_authtoken()
    if not token:
        print("\n🔑 NGROK AUTHTOKEN ERFORDERLICH:")
        print("1. Registriere dich kostenlos auf https://dashboard.ngrok.com/signup")
        print("2. Kopiere deinen Authtoken von https://dashboard.ngrok.com/get-started/your-authtoken")
        print("\nFüge deinen Authtoken unten ein (oder erstelle die Datei backend/ngrok_authtoken.txt):")
        try:
            user_token = input("Authtoken eingeben: ").strip()
            if user_token:
                with open(AUTHTOKEN_FILE, "w", encoding="utf-8") as f:
                    f.write(user_token)
                token = user_token
            else:
                print("❌ Kein Authtoken eingegeben. Beende...")
                sys.exit(1)
        except Exception:
            sys.exit(1)

    ngrok.set_auth_token(token)

    # Start Flask server in background thread
    flask_thread = threading.Thread(target=start_flask, daemon=True)
    flask_thread.start()

    time.sleep(2) # Give Flask a moment to start

    try:
        # Open an HTTP tunnel on port 5000
        public_url = ngrok.connect(PORT).public_url

        # Save active ngrok URL to file
        with open("backend/active_ngrok_url.txt", "w", encoding="utf-8") as f:
            f.write(public_url)

        print("\n✅ SUCCESS! ngrok Tunnel ist gestartet:")
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
