#!/bin/sh
./semantic_update_service.sh
SERVICE="java"

if [ $? -eq 0 ]
then
  ./kill-service.sh && ./start-service.sh
  echo "Successfully updated service"
else
  echo "No updates found"
fi

if pgrep -x "$SERVICE" >/dev/null
then
    echo "$SERVICE is running"
else
    echo "$SERVICE stopped, restarting"
    ./start-quadim.sh
fi
