/* simple example class that exercises parameter passing, field and variable accesses to examine resulting bytecodes */ 
public class TestOpsVarsFields
{
	private byte _b;
	private int _i;
	private float _f;
	private double _d;
	private Object _o1;
	private Object _o2;

	public static void main(String args[])
	{
		TestOpsVarsFields tovf = new TestOpsVarsFields();

		Object o = new Object();

		int x;

		x= tovf.m((byte)2,123,123.0f,123.0,o);
	}

	public int m(byte b, int i, float f, double d, Object o1)
	{
		_b = b;
		_i = i;
		_f = f;
		_d = d;
		_o1 = o1;
		_o2 = o1;

		if (i==0)
		{
			return _i;
		} 
		else
		{
			return i;
		}

	}

	public Object m2(Object o1, Object o2, Object o3)
	{
/*		Object o4;
		Object o5;
		Object o6;*/

		Object res;

		res= o1;
		res= o2;
		res= o3;

		/*res= 04;
		res= 05;
		res= 06;*/

		_o1 = o1;
		_o2 = o2;
		_o1 = o3;
		_o2 = res;
	

		return res;
	}

}