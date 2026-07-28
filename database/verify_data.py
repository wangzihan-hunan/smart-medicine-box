import psycopg2

USER = 'postgres'
PASSWORD = 'wzh@10086'
HOST = 'localhost'
PORT = '9456'
DATABASE_NAME = 'medicine_box'

def main():
    try:
        conn = psycopg2.connect(
            dbname=DATABASE_NAME,
            user=USER,
            password=PASSWORD,
            host=HOST,
            port=PORT
        )
        cursor = conn.cursor()
        
        print("=== Verifying Database Tables ===")
        
        cursor.execute("SELECT COUNT(*) FROM users")
        users_count = cursor.fetchone()[0]
        print(f"Users table: {users_count} records")
        
        cursor.execute("SELECT COUNT(*) FROM medicines")
        medicines_count = cursor.fetchone()[0]
        print(f"Medicines table: {medicines_count} records")
        
        cursor.execute("SELECT COUNT(*) FROM prescriptions")
        prescriptions_count = cursor.fetchone()[0]
        print(f"Prescriptions table: {prescriptions_count} records")
        
        cursor.execute("SELECT COUNT(*) FROM reminders")
        reminders_count = cursor.fetchone()[0]
        print(f"Reminders table: {reminders_count} records")
        
        cursor.execute("SELECT COUNT(*) FROM commands")
        commands_count = cursor.fetchone()[0]
        print(f"Commands table: {commands_count} records")
        
        print("\n=== Sample Data ===")
        
        print("\nUsers:")
        cursor.execute("SELECT id, phone, user_name, role, status FROM users")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]}, Phone: {row[1]}, Name: {row[2]}, Role: {row[3]}, Status: {row[4]}")
        
        print("\nMedicines:")
        cursor.execute("SELECT id, name, user_id, category, stock FROM medicines")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]}, Name: {row[1]}, UserID: {row[2]}, Category: {row[3]}, Stock: {row[4]}")
        
        print("\nPrescriptions:")
        cursor.execute("SELECT id, user_id, medicine_id, diagnosis FROM prescriptions")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]}, UserID: {row[1]}, MedicineID: {row[2]}, Diagnosis: {row[3]}")
        
        print("\nReminders:")
        cursor.execute("SELECT id, medicine_name, user_id, prescription_id, time, dosage, taken FROM reminders")
        for row in cursor.fetchall():
            print(f"  ID: {row[0]}, Medicine: {row[1]}, UserID: {row[2]}, PrescriptionID: {row[3]}, Time: {row[4]}, Dosage: {row[5]}, Taken: {row[6]}")
        
        print("\n=== Foreign Key Constraints ===")
        cursor.execute("""
            SELECT tc.constraint_name, kcu.table_name, kcu.column_name, 
                   ccu.table_name AS foreign_table_name, ccu.column_name AS foreign_column_name
            FROM information_schema.table_constraints tc
            JOIN information_schema.key_column_usage kcu 
                ON tc.constraint_name = kcu.constraint_name
            JOIN information_schema.constraint_column_usage ccu 
                ON ccu.constraint_name = tc.constraint_name
            WHERE tc.constraint_type = 'FOREIGN KEY'
        """)
        for row in cursor.fetchall():
            print(f"  {row[0]}: {row[1]}.{row[2]} -> {row[3]}.{row[4]}")
        
        print("\n=== Triggers ===")
        cursor.execute("""
            SELECT tgname, pg_class.relname AS table_name, tgtype
            FROM pg_trigger
            JOIN pg_class ON pg_trigger.tgrelid = pg_class.oid
            WHERE NOT tgisinternal
        """)
        for row in cursor.fetchall():
            print(f"  {row[0]} ON {row[1]}")
        
        print("\n=== Functions ===")
        cursor.execute("""
            SELECT proname, prorettype::regtype
            FROM pg_proc
            JOIN pg_namespace ON pg_proc.pronamespace = pg_namespace.oid
            WHERE nspname = 'public' AND prokind = 'f'
        """)
        for row in cursor.fetchall():
            print(f"  {row[0]} returns {row[1]}")
        
        print("\n=== Views ===")
        cursor.execute("""
            SELECT viewname
            FROM pg_views
            WHERE schemaname = 'public'
        """)
        for row in cursor.fetchall():
            print(f"  {row[0]}")
        
        cursor.close()
        conn.close()
        
        print("\n✅ Database verification completed successfully!")
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()