int main() {
   int x;
   x = 10;

   {
       string x = "hello";
       {
          double x = 2.3;

	  {
              bool x = false;
	  }
       }
   }
}
