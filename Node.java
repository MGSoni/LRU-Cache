package com.lld.tictactoe;

import java.util.HashMap;
import java.util.Map;

class Node {
    Node next;
    Node prev;
    int key;
    int value;

    Node( int value, int key){
        this.next = null;
        this.prev = null;
        this.value = value;
        this.key = key;
    }


}

public class DLL {
    Node head;
    Node tail;

    public void insertAtHead(Node node){
        if (head == null) {
            head = node;
            tail = node;
            node.prev = null;
            node.next = null;
            return;
        }
        head.prev = node;
        node.next = head;
        head = node;
        node.prev = null;
    }

    public void deleteNode(Node node){
        if(node == null){
            return;
        }
        if(node.prev == null && node.next == null){
            head = null;
            tail = null;
        } else if(node.prev == null){
            head = node.next;
            head.prev = null;
        }else if(node.next == null){
            tail = node.prev;
            tail.next = null;
        }else{
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
        node.prev = null;
        node.next = null;
    }

}

class LRUCache{
    Map<Integer,Node> map = new HashMap<>();
    DLL dll;
    int capacity;

    LRUCache(int capacity){
        this.dll = new DLL();
        this.capacity = capacity;
    }


}
