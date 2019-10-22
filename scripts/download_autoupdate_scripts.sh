wget https://raw.githubusercontent.com/Cantara/Whydah-SSOLoginWebApp/master/scripts/semantic_update_service.sh
chmod 755 semantic_update_service.sh 

wget https://raw.githubusercontent.com/Cantara/Whydah-SSOLoginWebApp/master/scripts/download_and_restart_if_new.sh
chmod 755 semantic_update_service.sh 

wget https://raw.githubusercontent.com/Cantara/Whydah-SSOLoginWebApp/master/scripts/CRON

echo "Use crontab CRON" to use cron to drive semantic auto-update of this service"
cat CRON
