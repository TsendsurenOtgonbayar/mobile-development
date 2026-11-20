import 'package:cloud_firestore/cloud_firestore.dart';
import '../models/transaction_item.dart';

class TransactionService {
  final String uid;

  TransactionService(this.uid);

  CollectionReference<Map<String, dynamic>> get _col => FirebaseFirestore
      .instance
      .collection('users')
      .doc(uid)
      .collection('transactions');

  Stream<List<TransactionItem>> watchTransactions() {
    return _col
        .orderBy('date', descending: true)
        .snapshots()
        .map((snap) => snap.docs.map(TransactionItem.fromDoc).toList());
  }

  Future<void> addTransaction(TransactionItem item) {
    return _col.add(item.toMap());
  }

  Future<void> deleteTransaction(String id) {
    return _col.doc(id).delete();
  }
}
