package bright.obb.infer;

import java.util.Vector;


class MessageQueue {
    
    private Vector v;

    public MessageQueue(){
	v = new Vector();
    }

    public void put(Message msg){
	v.addElement(msg);
    }

    public Message get(){
	Message msg = (Message) v.firstElement();
	v.removeElementAt(0);
	return msg;
    }

    public final int count(){ 
	return v.size();
    }

}
