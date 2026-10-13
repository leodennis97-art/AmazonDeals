# 🛒 AmazonDeals 24/7 Monitor & Gutschein-Alarm App

Android App & Cloud/Local Scraper Backend zur automatischen Überwachung von Amazon.de Gutscheinen (insbesondere **0.01 € Coupons / "Erhalte 2 für 0.01 €" Glitches** & **50 % Rabatt-Aktionen**).

---

## ⚡ Schnellstart mit ngrok (Lokaler Backend-Server)

Wenn du das Scraper-Backend direkt auf deinem PC laufen lassen möchtest (ohne Cloud-Deployment):

### 1️⃣ Lokales Backend mit ngrok starten
1. Navigiere in den Ordner `backend/`.
2. Führe die Datei `start_local_ngrok.bat` aus (oder im Terminal `python start_ngrok.py`).
3. Das Skript startet das Scraper-Backend und erstellt automatisch einen **ngrok Tunnel**.
4. Kopiere die angezeigte ngrok URL (z. B. `https://a1b2-34-56-78-90.ngrok-free.app`).

### 2️⃣ URL in der App eintragen
1. Öffne die **AmazonDeals** App.
2. Tippe oben rechts auf das ⚙️ **Einstellungen-Symbol**.
3. Füge die **ngrok URL** ein und klicke auf **Speichern**.

Die Android App verbindet sich ab sofort direkt mit deinem lokalen Scraper und überspringt automatisch den ngrok-Warnhinweis!

---

## ☁️ Dauerhaftes 24/7-Hosting auf Render.com

Falls du den PC nicht dauerhaft anlassen möchtest:

### 1️⃣ Code auf GitHub hochladen
```bash
git init
git add .
git commit -m "AmazonDeals mit ngrok & Cloud Support"
git branch -M main
git remote add origin https://github.com/DEIN_BENUTZERNAME/AmazonDeals.git
git push -u origin main
```

### 2️⃣ Auf Render.com veröffentlichen (100% Kostenlos)
1. Registriere dich auf [render.com](https://render.com).
2. Klicke auf **New +** -> **Web Service** und wähle das Repository.
3. Einstellungen:
   - **Root Directory:** `backend`
   - **Runtime:** `Python 3`
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `gunicorn app:app`
   - **Plan:** `Free`
4. Trage die Render-URL in den App-Einstellungen ein.
