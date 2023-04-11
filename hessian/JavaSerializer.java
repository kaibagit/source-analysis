class JavaSerializer{

  public void writeObject(Object obj, AbstractHessianOutput out)
  {
  	。。。

  	int ref = out.writeObjectBegin(cl.getName());

    if (ref < -1) {
      writeObject10(obj, out);
    }

 	。。。
  }

  protected void writeObject10(Object obj, AbstractHessianOutput out)
  {
    for (int i = 0; i < _fields.length; i++) {
      Field field = _fields[i];

      out.writeString(field.getName());

      _fieldSerializers[i].serialize(out, obj, field);
    }
      
    out.writeMapEnd();
  }

}