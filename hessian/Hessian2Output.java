class Hessian2Output{

  public void writeObject(Object object)
    throws IOException
  {
    if (object == null) {
      writeNull();
      return;
    }

    Serializer serializer;

    serializer = _serializerFactory.getSerializer(object.getClass());

    serializer.writeObject(object, this);
  }
  
}