import 'package:flutter/material.dart';
//import 'login_screen.dart';

class OnboardingScreen extends StatelessWidget {
  const OnboardingScreen({super.key});

  static const Color primary = Color(0xFF4A8C84);
  static const Color primaryLight = Color(0xFF6BB5AC);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F9F8),
      body: SafeArea(
        child: 
           Column(
            children: [
              const Spacer(flex: 1),  
              Stack(
                alignment: Alignment.center,   
                clipBehavior: Clip.none,    
                children: [
                  Container(
                    width: double.infinity,
                    height: 450,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: Color(0xFFEEF8F9),
                    )
                  ),
                  Image.asset(
                    'assets/image/Man.png',
                    height: 400,
                    fit: BoxFit.contain,
                  ),                 
                  Positioned(
                    top:85,   
                    right: 40, 
                    child: Container(
                      child: Image.asset(
                        'assets/image/Donut.png',
                        height: 70,
                        width: 70
                      ),
                    ),
                  ),
                  Positioned(
                    top:30,
                    left: 55,
                    child: Container(
                      child: Image.asset(
                        'assets/image/Coint.png',
                        height:70,
                        width:70
                      ),
                    ),
                  ),
                ],
              ),
              
              const SizedBox(height: 32),
              const Text(
                'Ухаалаг Зарцуулж \n Илүү Хэмнэ',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 30,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF438883),
                  height: 1.3,
                ),
              ),
              const Spacer(flex: 1),
          Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
            child: GestureDetector(
              onTap: () {
                  Navigator.pushNamed(context,'/login');
                },
              child: Container(
                width: double.infinity,
                height: 54,
                decoration: BoxDecoration(
                gradient: const LinearGradient(
                colors: [
                  Color(0xFF22C59E), 
                  Color(0xFF438883),
                ],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
              borderRadius: BorderRadius.circular(25),
              boxShadow: [
                BoxShadow(
                  color: Color(0xFF438883).withOpacity(0.4),
                  blurRadius: 25,
                  offset: const Offset(0, 55),
                ),
              ],
            ),
            alignment: Alignment.center,
            child: const Text(
              'Эхлэх',
              style: TextStyle(
                color: Colors.white,
                fontSize: 17,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ),
      ),
        const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text(
                    'Хэрэглэгчийн эрх байгаа юу?',
                    style: TextStyle(color: Colors.black),
                  ),
                  TextButton(
                    onPressed: () {
                        Navigator.pushNamed(context,'/register');
                      },
                    child: const Text(
                      'Бүртгүүлэх.',
                      style: TextStyle(color: Color(0xFF438883)),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      
    );
  }
}
