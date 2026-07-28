import psycopg2
import bcrypt

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
        
        new_password = '123456'
        hashed_password = bcrypt.hashpw(new_password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
        
        print(f"Generating BCrypt hash for password '123456':")
        print(f"Hash: {hashed_password}")
        
        cursor.execute("UPDATE users SET password = %s WHERE phone = %s", (hashed_password, '13800138000'))
        cursor.execute("UPDATE users SET password = %s WHERE phone = %s", (hashed_password, '13900139000'))
        cursor.execute("UPDATE users SET password = %s WHERE phone = %s", (hashed_password, '18800188000'))
        
        conn.commit()
        print("\n✅ Passwords updated successfully for all test users!")
        
        cursor.close()
        conn.close()
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()