# 💥 CitronDB - A library to easily handle MySQL  
This library is made for lazy people like me who don't want to  
bother using SQL syntaxes to manipulate a database in a project.  
  
If you have some SQL skills and your time is not that valuable,  
maybe you should do without this library, especially if performance is expected.  

## Maven
```
<dependency>
    <groupId>io.github.360matt</groupId>
    <artifactId>CitronDB</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## :blue_heart: Avantages:  
* Faster to develop your project  
* Easier to handle your database without too much effort  

## :smiling_imp: Disadvantages:  
* Operations are a bit slower than static queries  
* You don't learn SQL  


## ⭐ Beginning:
At the very beginning, you need to initialize the connection to the database by creating a Database instance.  
You can create as many as you want simultaneously. 
```java
Database db1 = new Database()
        .setHost(" ")
        .setPort(3306)
        .setDbName(" ")
        .setUsername(" ")
        .setPassword(" ")
        .connect();
}};
```  

You can end a connection at any time:
```java
db1.close();
```

## :blue_heart: :green_heart: :heart: Create a structure: 
There is no implementation or extensions needed for the class that will serve as a structure.  
You simply create fields, with optionally default values 

There are three annotations that you will find useful, all of which have the argument size=int.  
You apply them to the fields concerned   

* @Unique
* @Primary
* @Size(size=int)

```java
    public static class ExampleStucture {
        @Unique
        @Size(size = 50)
        public String test = "truc";
        public String truc = "jaaj";
        @Size(size = 20)
        public int caillou = 0;
    }
```

## Now let's get down to the basics !
### Initialize a tableManager:  
AnyClass, is as an example, it represents your structure class.  
The first argument will be the name of the table, and the second a reference to the structure class.  
```java
TableManager< AnyClass > tableManager = db2.getTable(" yourTableName ", AnyClass.class);
```  
  
When you read this sentence you have already done the hard part.  

### :heavy_plus_sign: Create table
When you create the table, the structure of your class automatically applies to the remote database table.  

If the table already exists, no error will be invoked.  
In the SQL query it is specified to create the table only if it does not exist.  

```java
tableManager.createTable();
// Create the table with the structure, and do nothing afterwards. 


tableManager.createTable(true);
/* Create the table with the structure if it doesn't exist.
   Otherwise update the columns to keep the table schema up to date.
   It is equivalent to an updateStructure(). */
```

### :heavy_division_sign: Update table:
At any time, you can update from your local structure.  
Usually this method is called in the createTable (true).  
But depending on what you want to do in your project, a call may be necessary.  
```java
tableManager.updateStructure();
```  
### :heavy_minus_sign: Delete table:
You can delete the table, but remember to recreate it if you plan to reuse it in the library afterwards.  
Since it will not recreate itself.  
```java
tableManager.deleteTable();
```
### :pencil2: Insert line:  
Using the structure class:  
```java
AnyClass element = new AnyClass();
element.someField = "some value";
element.id = 15; // example
element.email = "matt@risitas.es"

tableManager.insert( element );
```
Using Map<String, Object>:   
```java
Map<String, Object> builder = new Map<String, Object>();
builder.put("someField", "some value")
builder.put("id", 15)
builder.put("email", "matt@risitas.es");
        
tableManager.insert( builder );
```
### :scissors: Update lines:
To modify rows you must use two Map<String, Object> instances.  
One which will serve as a pattern, and the other which will serve to contain the modifications.  
```java
Map<String, Object> pattern = new Map<String, Object>();
pattern.put("id", 15);

Map<String, Object> modifications = new Map<String, Object>()
modifications.put("someField", " the new value ");
        
tableManager.update(pattern, modification);
// will set someField="the new value" WHERE id=15
```
### :grey_question: Get one line (or first line):
To retrieve the row, you must use a Map<String, Object> to define a search pattern.  
Be careful, the result can be null if no row has been found.  
the result will be an instance of the class structure:  
```java

Map<String, Object> pattern = new Map<String, Object>();
pattern.put("email", "matt@risitas.es");
// search line where email="matt@risitas.es"

AnyClass element = tableManager.getLine( pattern );
```
### :grey_question: Get multiple lines:
Similar to the example above.
But the output data (instances of the class structure) will be returned in a Set<?> Collection.
```java
// find all line where country=France
Map<String, Object> pattern = new Map<String, Object>();
pattern.put("country", "France");
        
        
Set<AnyClass> lines = tableManager.getLines(pattern);
// get unlimited lines


Set<AnyClass> lines = tableManager.getLines(pattern, 20);
// get limited lines, here 20.
```

### :recycle: Remove lines:
You can delete the lines corresponding to the pattern:
```java
// find all disabled user account for example.
Map<String, Object> pattern = new Map<String, Object>();
pattern.put("disabledAccount", true);

tableManager.remove( pattern );
```
