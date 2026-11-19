import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState()=> _RegisterScreenState();
}
class _RegisterScreenState extends State<RegisterScreen>{
  bool _obscurePassword=true;
  final _fullName= TextEditingController();
  final _email= TextEditingController();
  final _password= TextEditingController();
  final _secPassword= TextEditingController();
  bool _isLoading= false;
  String? _errorMessage;
  @override
  void dispose(){
    _fullName.dispose();
    _email.dispose();
    _password.dispose();
    _secPassword.dispose();
    super.dispose();
  }
  Future<void> _register() async{ 
    final fullName= _fullName.text.trim();
    final email=_email.text.trim();
    final password= _password.text.trim();
    final secondPassword= _secPassword.text.trim();
    if (password!=secondPassword) {
       setState(()=> _errorMessage='password buruu bn');
       return;
    } 
    if (fullName.isEmpty || email.isEmpty || password.isEmpty) {
      setState(()=> _errorMessage='Talbaruudaa zuv boglo');
      return;
    }
    if (password.length<6) {
      setState(()=> _errorMessage='Password 6 temdegt aguulsan baih');
      return;
    }
    setState((){
      _isLoading=true;
      _errorMessage=null;
    });
    try {
    final credential = await FirebaseAuth.instance.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );  
    final user = credential.user;
      if (user != null) {
        await user.updateDisplayName(fullName);
        await user.reload();

        await FirebaseFirestore.instance
            .collection('users')
            .doc(user.uid)
            .set({
          'uid': user.uid,
          'fullName': fullName,
          'email': email,
          'createdAt': FieldValue.serverTimestamp(),
        });
        }
    } on FirebaseAuthException catch(e){
      setState(()=> _errorMessage='$e.code'); 
    }catch (e) {
      setState(()=> _errorMessage='$e');
    } finally{
      if (mounted) setState(()=> _isLoading=false);
    }
  }
  @override
  Widget build(BuildContext context) {
    final size =MediaQuery.sizeOf(context);

      return Scaffold(
      backgroundColor: Colors.white,
      body: Stack(
        children: [
          Positioned(
            top: -40,
            left: 0,
            right: 0,
            child: SvgPicture.asset(
              'assets/image/header.svg',
              width: size.width,
              height: 320,
            ),
          ),
          Positioned(
            top: 0,
            left: 0,
            child: Image.asset(
              'assets/image/under_header.png',
              width: 150,
              height: 150,
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const SizedBox(height: 80),
                  const Text(
                    'Тавтай морилно уу?',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Орлого, зарлагаа хянахад тань тусална',
                    style: TextStyle(color: Colors.grey),
                  ),
                  const SizedBox(height: 150),
                  TextField(
                    controller: _fullName,
                    decoration: InputDecoration(
                      hintText: 'Бүтэн нэрээ оруулна уу?',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 16,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller:_email,
                    decoration: InputDecoration(
                      hintText: 'Имэйлээ оруулна уу?',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 16,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    obscureText: _obscurePassword,
                    controller: _password,
                    decoration: InputDecoration(
                      hintText: 'Нууц үгээ оруулаарай',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 16,
                      ),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_off : Icons.visibility,
                          color: Colors.grey,
                        ),
                        onPressed: () {
                          setState(() {
                            _obscurePassword = !_obscurePassword;
                          });
                        },
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _secPassword,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      hintText: 'Нууц үгээ давтан оруулаарай',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 16,
                      ),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword ? Icons.visibility_off : Icons.visibility,
                          color: Colors.grey,
                        ),
                        onPressed: () {
                          setState(() {
                            _obscurePassword = !_obscurePassword;
                          });
                        },
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),
                  GestureDetector(
                    onTap: () {
                      _isLoading ? null : _register();
                     // Navigator.pushNamed(context,'/login');
                    },
                    child: Container(
                      width: double.infinity,
                      height: 54,
                      decoration: BoxDecoration(
                        gradient:  LinearGradient(
                          colors: _isLoading? [Colors.grey,Colors.grey]
                          :[Color(0xFF22C59E), Color(0xFF438883),],
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                        ),
                        borderRadius: BorderRadius.circular(25),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFF438883).withOpacity(0.4),
                            blurRadius: 25,
                            offset: const Offset(0, 50),
                          ),
                        ],
                      ),
                      alignment: Alignment.center,
                      child: _isLoading? 
                      const SizedBox(
                        height:17,
                        width:17,
                        child: CircularProgressIndicator(
                          strokeWidth:2,
                          color: Colors.white,
                        )
                      )
                      :const Text(
                        'Бүртгүүлэх',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Text('Хэрэглэгчийн эрх байгаа юу? '),
                      TextButton(
                        onPressed: () {
                          Navigator.pushNamed(context, '/login');
                        },
                        child: const Text(
                          'Нэвтрэх',
                          style: TextStyle(
                            color: Color(0xFF1A9B7A),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 30),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
