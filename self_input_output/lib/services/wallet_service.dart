import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';

import '../models/pending_payment.dart';
import '../models/transaction_item.dart';
import 'pending_payment_service.dart';
/// Dropdown-д харагдах гүйлгээний төрөл (Netflix, Youtube, Spotify)
class ExpenseType {
  final String id;
  final String title;
  final String iconKey;

  const ExpenseType({
    required this.id,
    required this.title,
    required this.iconKey,
  });

  // PendingPayment-ийн icon хөрвүүлэлтийг дахин ашиглана
  IconData get icon => PendingPayment(
        id: '',
        title: title,
        amount: 0,
        dueDate: DateTime.now(),
        iconKey: iconKey,
      ).icon;
}

class WalletService {
  final String uid;
  WalletService(this.uid);

  final FirebaseFirestore _db = FirebaseFirestore.instance;

  DocumentReference<Map<String, dynamic>> get _user =>
      _db.collection('users').doc(uid);

  // ---------- Картууд ----------
  // ЛАБОРАТОРИЙН ТУРШИЛТ: жинхэн картын мэдээлэл оруулахгүй
  Future<void> addCard({
    required String holderName,
    required String cardNumber,
    required String cvc,
    required String expiry,
    required String value,
  }) {
    return _user.collection('cards').add({
      'holderName': holderName,
      'cardNumber': cardNumber,
      'cvc': cvc,
      'expiry': expiry,
      'value': value,
      'createdAt': FieldValue.serverTimestamp(),
    });
  }

  // ---------- Аккаунт холбох ----------
  // linkedAccounts-д хадгалаад, нэг дор +1000$ income гүйлгээ нэмнэ
  Future<void> linkAccount(String type) {
    final batch = _db.batch();

    batch.set(_user.collection('linkedAccounts').doc(), {
      'type': type,
      'createdAt': FieldValue.serverTimestamp(),
    });

    final income = TransactionItem(
      id: '',
      title: 'Данс холбосон ($type)',
      date: DateTime.now(),
      amount: 1000,
      isIncome: true,
    );
    batch.set(_user.collection('transactions').doc(), income.toMap());

    return batch.commit();
  }

  // ---------- Гүйлгээний төрлүүд ----------
  Stream<List<ExpenseType>> watchExpenseTypes() {
    return _user.collection('expenseTypes').orderBy('title').snapshots().map(
          (snap) => snap.docs
              .map((d) => ExpenseType(
                    id: d.id,
                    title: d.data()['title'] ?? '',
                    iconKey: d.data()['iconKey'] ?? 'other',
                  ))
              .toList(),
        );
  }

  // Нэг удаа л үүснэ. Хоосон байвал Netflix, Youtube, Spotify нэмнэ
  Future<void> seedExpenseTypesIfEmpty() async {
    final col = _user.collection('expenseTypes');
    final snap = await col.limit(1).get();
    if (snap.docs.isNotEmpty) return;

    const types = [
      {'title': 'Netflix', 'iconKey': 'netflix'},
      {'title': 'Youtube', 'iconKey': 'youtube'},
      {'title': 'Spotify', 'iconKey': 'music'},
    ];
    final batch = _db.batch();
    for (final t in types) {
      batch.set(col.doc(t['title']!.toLowerCase()), t);
    }
    await batch.commit();
  }

  // ---------- Төлбөр нэмэх ----------
  // pending-д орсноор "Хүлээгдэж буй гүйлгээ" tab дээр шууд харагдана
  Future<void> addPending(PendingPayment p) =>
    PendingPaymentService(uid).addPending(p);
}
