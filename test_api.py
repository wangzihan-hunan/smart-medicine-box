import requests

def test_login():
    try:
        url = 'http://localhost:8080/api/auth/login'
        payload = {
            'phone': '13800138000',
            'password': '123456'
        }
        response = requests.post(url, json=payload)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_register():
    try:
        url = 'http://localhost:8080/api/auth/register'
        payload = {
            'phone': '13700137000',
            'password': '123456',
            'name': 'Test User'
        }
        response = requests.post(url, json=payload)
        print(f"Register Status Code: {response.status_code}")
        print(f"Register Response: {response.text}")
        return response.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False

if __name__ == '__main__':
    print("Testing Login API...")
    login_success = test_login()
    
    print("\nTesting Register API...")
    register_success = test_register()
    
    if login_success:
        print("\n✅ Login API works correctly!")
    else:
        print("\n❌ Login API failed")
        
    if register_success:
        print("✅ Register API works correctly!")
    else:
        print("❌ Register API failed")