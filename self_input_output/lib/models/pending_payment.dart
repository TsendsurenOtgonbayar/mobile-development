import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';

class PendingPayment {
  final String id;
  final String title;
  final double amount;
  final DateTime dueDate;
  final String iconKey;

  const PendingPayment({
    required this.id,
    required this.title,
    required this.amount,
    required this.dueDate,
    this.iconKey = 'other',
  });

  // Firestore-д зөвхөн текст түлхүүр хадгалж, энд icon руу хөрвүүлнэ
  static const Map<String, IconData> _icons = {
    'youtube': Icons.smart_display,
    'electricity': Icons.bolt,
    'rent': Icons.home,
    'music': Icons.music_note,
    'other': Icons.receipt_long,
  };

  IconData get icon => _icons[iconKey] ?? Icons.receipt_long;

  factory PendingPayment.fromDoc(
      QueryDocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data();
    final ts = data['dueDate'];
    return PendingPayment(
      id: doc.id,
      title: data['title'] ?? '',
      amount: (data['amount'] ?? 0).toDouble(),
      dueDate: ts is Timestamp ? ts.toDate() : DateTime.now(),
      iconKey: data['iconKey'] ?? 'other',
    );
  }

  Map<String, dynamic> toMap() => {
        'title': title,
        'amount': amount,
        'dueDate': Timestamp.fromDate(dueDate),
        'iconKey': iconKey,
      };
}
