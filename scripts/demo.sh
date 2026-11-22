#!/usr/bin/env bash
# Walks one guest through search -> book -> pay -> cancel against a running service.
# Usage: scripts/demo.sh [base-url]
set -euo pipefail
BASE=${1:-http://localhost:8080}
IN=$(date -v+20d +%F 2>/dev/null || date -d "+20 days" +%F)
OUT=$(date -v+22d +%F 2>/dev/null || date -d "+22 days" +%F)
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }

echo "== search: 5-star in Indiranagar with wifi"
RESULT=$(curl -s "$BASE/api/properties/search?city=Bengaluru&locality=Indiranagar&checkIn=$IN&checkOut=$OUT&guests=2&minStars=5&amenities=wifi")
echo "$RESULT" | json "[ (p['name'], [o['roomType'] for o in p['offers']]) for p in d ]"
PROPERTY=$(echo "$RESULT" | json "d[0]['propertyId']")
ROOM=$(echo "$RESULT" | json "d[0]['offers'][0]['roomTypeId']")

echo "== book"
BOOKING=$(curl -s -XPOST "$BASE/api/bookings" -H 'Content-Type: application/json' -d \
  "{\"propertyId\":\"$PROPERTY\",\"roomTypeId\":\"$ROOM\",\"checkIn\":\"$IN\",\"checkOut\":\"$OUT\",\"guests\":2,\"guestName\":\"Ria\",\"guestEmail\":\"ria@example.com\"}")
echo "$BOOKING" | json "(d['id'], d['status'], d['totalPrice'])"
ID=$(echo "$BOOKING" | json "d['id']")

echo "== pay (declined card first, then UPI)"
curl -s -XPOST "$BASE/api/bookings/$ID/payments" -H 'Content-Type: application/json' \
  -d '{"method":"CARD","details":{"cardNumber":"4111111111110000","expiry":"12/30"}}' | json "(d['paymentStatus'], d['bookingStatus'], d['failureReason'])"
curl -s -XPOST "$BASE/api/bookings/$ID/payments" -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"method":"UPI","details":{"vpa":"ria@okbank"}}' | json "(d['paymentStatus'], d['bookingStatus'], d['instrument'])"

echo "== cancel"
curl -s -XPOST "$BASE/api/bookings/$ID/cancel" | json "(d['booking']['status'], 'refund', d['refund'])"
