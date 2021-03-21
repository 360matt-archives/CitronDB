# 💥 CitronDB - A library to easily handle MySQL

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

## Create a structure:
There is no implementation or extensions needed for the class that will serve as a structure.  
You simply create fields, with optionally default values 

There are three annotations that you will find useful, all of which have the argument size=int.  
You apply them to the fields concerned   

* @Unique(size=int)
* @Primary(size=int)
* @Size(size=int)

```java
    public static class ExampleStucture {
        @Unique(size = 50)
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

### Create table
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

### Update table:
At any time, you can update from your local structure.  
Usually this method is called in the createTable (true).  
But depending on what you want to do in your project, a call may be necessary.  
```java
tableManager.updateStructure();
```  
  
### Insert line:  
Using the structure class:  
```java
AnyClass element = new AnyClass();
element.someField = "some value";
element.id = 15; // example
element.email = "matt@risitas.es"

tableManager.insert( element );
```
Using the RowBuilder:   
```java
RowBuilder builder = new RowBuilder()
        .define("someField", "some value")
        .define("id", 15)
        .define("email", "matt@risitas.es");
        
tableManager.insert( builder );
```
### Update lines:
To modify rows you must use two RowBuilder instances.  
One which will serve as a pattern, and the other which will serve to contain the modifications.  
```java
RowBuilder pattern = new RowBuilder()
        .define("id", 15);

RowBuilder modifications = new RowBuilder()
        .define("someField", " the new value ");
        
tableManager.update(pattern, modification);
// will set someField="the new value" WHERE id=15
```
