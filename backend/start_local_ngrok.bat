@echo off
title AmazonDeals Local Backend + ngrok
cd /d %~dp0
echo Installing requirements...
pip install -r requirements.txt
echo Starting Scraper & ngrok Tunnel...
python start_ngrok.py
pause
