-- Fix for order status constraint issue
-- This script removes the old integer-based check constraint and updates the column to VARCHAR

-- Step 1: Drop the existing check constraint
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- Step 2: Convert existing integer status values to string enum names (if any data exists)
-- Map ordinal values to enum names:
-- 0=PENDING, 1=CREATED, 2=STOCK_RESERVED, 3=STOCK_FAILED, 4=PAYMENT_PENDING, 
-- 5=PAID, 6=PAYMENT_FAILED, 7=SHIPPING_PENDING, 8=SHIPPED, 9=SHIPPING_FAILED, 
-- 10=CANCELLED, 11=PAYMENT_COMPLETED, 12=INVENTORY_RESERVED, 13=CONFIRMED, 
-- 14=FAILED, 15=OUT_OF_STOCK, 16=COMPLETED

-- First, alter the column type to VARCHAR if it's currently INTEGER
ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(50);

-- Update existing records (if any) - map integers to string values
-- Note: This assumes the column currently contains integer values
-- If the table is empty or already contains strings, these updates will have no effect

UPDATE orders SET status = 'PENDING' WHERE status = '0';
UPDATE orders SET status = 'CREATED' WHERE status = '1';
UPDATE orders SET status = 'STOCK_RESERVED' WHERE status = '2';
UPDATE orders SET status = 'STOCK_FAILED' WHERE status = '3';
UPDATE orders SET status = 'PAYMENT_PENDING' WHERE status = '4';
UPDATE orders SET status = 'PAID' WHERE status = '5';
UPDATE orders SET status = 'PAYMENT_FAILED' WHERE status = '6';
UPDATE orders SET status = 'SHIPPING_PENDING' WHERE status = '7';
UPDATE orders SET status = 'SHIPPED' WHERE status = '8';
UPDATE orders SET status = 'SHIPPING_FAILED' WHERE status = '9';
UPDATE orders SET status = 'CANCELLED' WHERE status = '10';
UPDATE orders SET status = 'PAYMENT_COMPLETED' WHERE status = '11';
UPDATE orders SET status = 'INVENTORY_RESERVED' WHERE status = '12';
UPDATE orders SET status = 'CONFIRMED' WHERE status = '13';
UPDATE orders SET status = 'FAILED' WHERE status = '14';
UPDATE orders SET status = 'OUT_OF_STOCK' WHERE status = '15';
UPDATE orders SET status = 'COMPLETED' WHERE status = '16';

-- Step 3: Add a new check constraint for valid string enum values (optional but recommended)
ALTER TABLE orders ADD CONSTRAINT orders_status_check 
CHECK (status IN (
    'PENDING', 'CREATED', 'STOCK_RESERVED', 'STOCK_FAILED', 
    'PAYMENT_PENDING', 'PAID', 'PAYMENT_FAILED', 
    'SHIPPING_PENDING', 'SHIPPED', 'SHIPPING_FAILED', 
    'CANCELLED', 'PAYMENT_COMPLETED', 'INVENTORY_RESERVED', 
    'CONFIRMED', 'FAILED', 'OUT_OF_STOCK', 'COMPLETED'
));

-- Verify the changes
SELECT 
    column_name, 
    data_type, 
    character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'orders' AND column_name = 'status';

-- Show any existing orders to verify status values
SELECT id, status, created_at FROM orders LIMIT 10;
