import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState()=> _LoginScreenState();
}
class _LoginScreenState extends State<LoginScreen>{
  bool _obscurePassword=true;
  final _email=TextEditingController();
  final _password=TextEditingController();
  bool _isLoading=false;
  String? _errorMessage;
  
  @override
  void dispose(){
      _email.dispose();
      _password.dispose();
      super.dispose();
  }

  Future<void> _submit() async{
    final email=_email.text.trim();
    final password= _password.text.trim();
    if (email.isEmpty || password.isEmpty) {
      setState(()=>_errorMessage='email esvel password buruu bn');
      return;
    }
    if (password.length<6) {
      setState(()=> _errorMessage='password n dor hayj 6 temdegt baina');
      return;
    }
    setState((){
      _isLoading=true;
      _errorMessage=null;
    });
    try {
      await FirebaseAuth.instance.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
    }on FirebaseAuthException catch(e){
      setState((){
          _errorMessage='$e.code';
      });
    }catch (e) {
      setState((){
          _errorMessage='$e';
      });
    }
    finally{
      if(mounted) setState(()=>_isLoading=false);
    }
  }
  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;

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
            top:0,
            left:0,
            child: Image.asset(
              'assets/image/under_header.png',
              width:150,
              height:150,
            )
          ),
          Positioned(
            top: 150,
            right:50,
            child: Align(
              alignment: Alignment.center,
              child: Image.asset(
                'assets/image/header_logo.png',
                width:250,
                height:250,
              ),
            ),
          ),
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const SizedBox(height: 90),
                  const Text(
                    'Тавтай морилно уу !',
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 300), 
                  TextField(
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    decoration: InputDecoration(
                      hintText: 'Enter your email address',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    obscureText: _obscurePassword,
                    controller: _password,
                    decoration: InputDecoration(
                      hintText: 'Enter your password',
                      filled: true,
                      fillColor: Colors.grey.shade100,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(14),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
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
                  const SizedBox(height:5),
                  if (_errorMessage!=null) 
                    Text(
                      _errorMessage!,
                      style: const TextStyle(color:Colors.red, fontSize: 17),
                    ),
                  
                  const SizedBox(height: 40),
                  GestureDetector(
                    onTap: _isLoading ? null : _submit,
                    child: Container(
                      width: double.infinity,
                      height: 54,
                      decoration: BoxDecoration(
                        gradient:  LinearGradient(
                          colors: _isLoading? [Colors.grey,Colors.grey]: [
                            Color(0xFF22C59E), 
                            Color(0xFF438883),
                          ],
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                        ),
                        borderRadius: BorderRadius.circular(25),
                        boxShadow: [
                          BoxShadow(
                            color: const Color(0xFF438883).withOpacity(0.4),
                            blurRadius: 25,
                            offset: const Offset(0, 60),
                          ),
                        ],
                      ),
                      alignment: Alignment.center,
                      child: _isLoading ?
                      const SizedBox(
                        height: 17,
                        width:17,
                        child: CircularProgressIndicator(
                          strokeWidth:2,
                          color: Colors.white,
                        ),
                      )
                      :const Text(
                        'Нэвтрэх',
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
                      const Text('Бүртгэлгүй юу? '),
                      TextButton(
                        onPressed: () {
                          Navigator.pushNamedAndRemoveUntil(
                            context,
                            '/register',
                            (route)=>false,);
                        },
                        child: const Text(
                          'Бүртгүүлэх',
                          style: TextStyle(
                            color: Color(0xFF1A9B7A),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 40),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
