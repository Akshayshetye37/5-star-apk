# Hotel POS — V6 Native Remaining Features

V6 HTML is the exact master reference.

Remaining native implementation:
1. Made Food
2. Restaurant Inventory
3. Ready-made Inventory
4. Walk-in Restaurant Billing

Exact V6 requirements:
- Exact Aurelia UI
- Exact colors
- Exact buttons
- Exact labels
- Exact behavior
- Exact workflows
- Preserve existing payment/folio/invoice/PMS implementation
- Do not duplicate existing functionality
- Do not use the old APK UI as the design reference

Made Food:
- Add Food
- Edit
- Remove
- Selling price
- Unit/details
- Internal recipes may consume ingredients
- Raw ingredients remain internal

Restaurant Inventory:
- Add Ingredient
- Edit
- Purchase rate
- Unit
- Current stock
- Reorder threshold
- Purchases add to existing stock
- Purchases never overwrite history
- Low-stock warning

Ready-made Inventory:
- Add product
- Edit
- Purchase rate
- Selling price
- Unit
- Purchases add stock
- Sales reduce stock
- Purchase rate internal only

Walk-in Restaurant:
- Multiple simultaneous open bills
- Add Food
- Remove Food
- View Bill PDF
- Share Bill PDF
- WhatsApp
- Record Payment
- Close Bill
- OPEN/PARTIALLY_PAID/PAID/CLOSED
- No hotel room required

Customer restaurant display must show product names, quantities and selling totals only.
