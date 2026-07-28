import psycopg2
import os
import re

DATABASE_NAME = 'medicine_box'
USER = 'postgres'
PASSWORD = 'wzh@10086'
HOST = 'localhost'
PORT = '9456'

def parse_sql_statements(sql_content):
    statements = []
    current_statement = ''
    dollar_quotes = 0
    
    lines = sql_content.split('\n')
    for line in lines:
        stripped = line.strip()
        
        if stripped.startswith('--') or stripped.startswith('\\'):
            continue
        
        dollar_matches = re.findall(r'\$\$', stripped)
        dollar_quotes += len(dollar_matches)
        
        current_statement += line + '\n'
        
        if stripped.endswith(';') and dollar_quotes % 2 == 0:
            statements.append(current_statement.strip())
            current_statement = ''
    
    if current_statement.strip():
        statements.append(current_statement.strip())
    
    return statements

def execute_sql_file(conn, file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        sql_content = f.read()
    
    statements = parse_sql_statements(sql_content)
    cursor = conn.cursor()
    
    for i, statement in enumerate(statements):
        statement = statement.strip()
        if not statement:
            continue
        
        try:
            cursor.execute(statement)
            conn.commit()
            print(f"✓ Executed statement {i+1}")
        except Exception as e:
            print(f"✗ Error executing statement {i+1}: {str(e)[:200]}")
            conn.rollback()
    
    cursor.close()

def main():
    try:
        print("Connecting to PostgreSQL...")
        conn = psycopg2.connect(
            dbname='postgres',
            user=USER,
            password=PASSWORD,
            host=HOST,
            port=PORT
        )
        conn.autocommit = True
        cursor = conn.cursor()
        
        print("Closing existing connections to database...")
        cursor.execute(f"""
            SELECT pg_terminate_backend(pg_stat_activity.pid)
            FROM pg_stat_activity
            WHERE pg_stat_activity.datname = '{DATABASE_NAME}'
            AND pid <> pg_backend_pid()
        """)
        
        print("Dropping existing database if exists...")
        cursor.execute(f"DROP DATABASE IF EXISTS {DATABASE_NAME}")
        
        print(f"Creating database {DATABASE_NAME} with template0...")
        cursor.execute(f"""
            CREATE DATABASE {DATABASE_NAME}
            WITH OWNER = {USER}
            ENCODING = 'UTF8'
            LC_COLLATE = 'zh_CN.UTF-8'
            LC_CTYPE = 'zh_CN.UTF-8'
            TABLESPACE = pg_default
            CONNECTION LIMIT = -1
            TEMPLATE = template0
        """)
        
        cursor.close()
        conn.close()
        
        print(f"Connecting to {DATABASE_NAME}...")
        conn = psycopg2.connect(
            dbname=DATABASE_NAME,
            user=USER,
            password=PASSWORD,
            host=HOST,
            port=PORT
        )
        conn.autocommit = False
        
        script_path = os.path.join(os.path.dirname(__file__), 'init.sql')
        
        print(f"Executing SQL script: {script_path}")
        execute_sql_file(conn, script_path)
        
        conn.close()
        print("\n✅ Database initialization completed successfully!")
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()