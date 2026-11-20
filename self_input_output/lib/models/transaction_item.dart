import 'package:cloud_firestore/cloud_firestore.dart';

class TransactionItem {
  final String id;
  final String title;
  final DateTime date;
  final double amount;
  final bool isIncome;

  const TransactionItem({
    required this.id,
    required this.title,
    required this.date,
    required this.amount,
    required this.isIncome,
  });

  // Firestore document -> TransactionItem
  factory TransactionItem.fromDoc(
      QueryDocumentSnapshot<Map<String, dynamic>> doc) {
    final data = doc.data();
    final ts = data['date'];
    return TransactionItem(
      id: doc.id,
      title: data['title'] ?? '',
      amount: (data['amount'] ?? 0).toDouble(),
      isIncome: data['isIncome'] ?? false,
      date: ts is Timestamp ? ts.toDate() : DateTime.now(),
    );
  }

  // TransactionItem -> Firestore (id нь document-ийн нэр тул орохгүй)
  Map<String, dynamic> toMap() => {
        'title': title,
        'amount': amount,
        'isIncome': isIncome,
        'date': Timestamp.fromDate(date),
      };
}
