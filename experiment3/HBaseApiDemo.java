import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class HBaseApiDemo {

    private static Connection connection;

    static {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "127.0.0.1");
            conf.set("hbase.zookeeper.property.clientPort", "2181");
            connection = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createTable(String tableName, String[] fields) throws IOException {
        Admin admin = connection.getAdmin();
        TableName tn = TableName.valueOf(tableName);
        if (admin.tableExists(tn)) {
            admin.disableTable(tn);
            admin.deleteTable(tn);
        }
        HTableDescriptor tableDesc = new HTableDescriptor(tn);
        for (String family : fields) {
            HColumnDescriptor colDesc = new HColumnDescriptor(Bytes.toBytes(family));
            tableDesc.addFamily(colDesc);
        }
        admin.createTable(tableDesc);
        System.out.println("表[" + tableName + "]创建完成");
        admin.close();
    }

    public static void addRecord(String tableName, String row, String[] fields, String[] values) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Put put = new Put(Bytes.toBytes(row));
        if (fields.length != values.length) {
            throw new RuntimeException("fields与values数组长度不一致");
        }
        for (int i = 0; i < fields.length; i++) {
            String[] parts = fields[i].split(":");
            String family = parts[0];
            String qualifier = parts[1];
            put.addColumn(
                    Bytes.toBytes(family),
                    Bytes.toBytes(qualifier),
                    Bytes.toBytes(values[i])
            );
        }
        table.put(put);
        table.close();
        System.out.println("行[" + row + "]插入完成");
    }

    public static void scanColumn(String tableName, String column) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Scan scan = new Scan();
        if (column.contains(":")) {
            String[] arr = column.split(":");
            scan.addColumn(Bytes.toBytes(arr[0]), Bytes.toBytes(arr[1]));
        } else {
            scan.addFamily(Bytes.toBytes(column));
        }
        ResultScanner scanner = table.getScanner(scan);
        System.out.println("==== scanColumn result for [" + column + "] ====");
        for (Result res : scanner) {
            Cell[] cells = res.rawCells();
            for (Cell cell : cells) {
                String rowKey = Bytes.toString(CellUtil.cloneRow(cell));
                String fam = Bytes.toString(CellUtil.cloneFamily(cell));
                String qual = Bytes.toString(CellUtil.cloneQualifier(cell));
                String val = Bytes.toString(CellUtil.cloneValue(cell));
                System.out.printf("row=%s , %s:%s = %s%n", rowKey, fam, qual, val);
            }
        }
        scanner.close();
        table.close();
    }

    public static void modifyData(String tableName, String row, String column, String newVal) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        String[] arr = column.split(":");
        String family = arr[0];
        String qualifier = arr[1];
        Put put = new Put(Bytes.toBytes(row));
        put.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(newVal));
        table.put(put);
        System.out.println("修改完成 row:" + row + " column:" + column + " -> " + newVal);
        table.close();
    }

    public static void deleteRow(String tableName, String row) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Delete del = new Delete(Bytes.toBytes(row));
        table.delete(del);
        System.out.println("删除行rowkey=" + row);
        table.close();
    }

    public static void main(String[] args) {
        try {
            createTable("Student", new String[]{"info"});

            String[] fields = {"info:S_Name", "info:S_Sex", "info:S_Age"};
            String[] val1 = {"Zhangsan", "male", "23"};
            addRecord("Student", "2015001", fields, val1);

            String[] val2 = {"Mary", "female", "22"};
            addRecord("Student", "2015002", fields, val2);

            String[] val3 = {"Lisi", "male", "24"};
            addRecord("Student", "2015003", fields, val3);

            System.out.println("\n-----①扫描整个列族info-----");
            scanColumn("Student", "info");

            System.out.println("\n-----②只扫描info:S_Name这一列-----");
            scanColumn("Student", "info:S_Name");

            modifyData("Student","2015001","info:S_Age","25");

            deleteRow("Student","2015003");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
