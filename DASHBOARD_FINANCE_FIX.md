# Dashboard finance fix

- Today's Revenue is calculated from positive payment-ledger entries whose payment timestamp is today.
- Pending Balance includes outstanding amounts on all non-cancelled bookings, including checked-out bookings.
- Revenue and Pending cards are tappable: Revenue opens Payment Ledger; Pending opens Invoice History.
- This does not alter the Room schema or stored records.
