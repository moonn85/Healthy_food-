package com.example.btlandroid.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandroid.Adapter.TopProductAdapter;
import com.example.btlandroid.Domain.CartItem;
import com.example.btlandroid.Domain.OrderDomain;
import com.example.btlandroid.Model.TopProduct;
import com.example.btlandroid.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IncomeStatisticsActivity extends AppCompatActivity {

    // Khai báo các thuộc tính
    private FirebaseDatabase database;
    private DatabaseReference ordersRef;
    
    private TextView totalRevenueTxt, totalOrdersTxt, noDataTxt, noProductsTxt;
    private RadioGroup timeRangeRadioGroup;
    private RadioButton todayRadioButton, weekRadioButton, monthRadioButton, yearRadioButton;
    private RecyclerView topProductsRecyclerView;
    private ProgressBar progressBar;
    private BarChart barChart;
    
    private TopProductAdapter topProductAdapter;
    private List<TopProduct> topProductList;
    private DecimalFormat formatter;
    
    private static final String TIME_RANGE_TODAY = "today";
    private static final String TIME_RANGE_WEEK = "week";
    private static final String TIME_RANGE_MONTH = "month";
    private static final String TIME_RANGE_YEAR = "year";
    private String currentTimeRange = TIME_RANGE_TODAY; // Mặc định là hôm nay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_income_statistics);
        
        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        ordersRef = database.getReference("Orders");
        
        // Khởi tạo định dạng số
        formatter = new DecimalFormat("#,###");
        
        // Ánh xạ các thành phần giao diện
        initViews();
        
        // Thiết lập sự kiện cho nút back
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        
        // Thiết lập RadioGroup cho việc chọn thời gian
        setupTimeRangeSelection();
        
        // Thiết lập RecyclerView cho danh sách sản phẩm bán chạy
        setupTopProductsRecyclerView();
        
        // Tải dữ liệu thống kê thu nhập
        loadIncomeStatistics(currentTimeRange);
    }

    // Phương thức khởi tạo các thành phần giao diện
    // Ánh xạ các view từ layout
    // Thiết lập RecyclerView, ProgressBar, BarChart và các TextView
    // Thiết lập các thuộc tính mặc định cho các thành phần giao diện
    private void initViews() {
        totalRevenueTxt = findViewById(R.id.totalRevenueTxt);
        totalOrdersTxt = findViewById(R.id.totalOrdersTxt);
        timeRangeRadioGroup = findViewById(R.id.timeRangeRadioGroup);
        todayRadioButton = findViewById(R.id.todayRadioButton);
        weekRadioButton = findViewById(R.id.weekRadioButton);
        monthRadioButton = findViewById(R.id.monthRadioButton);
        yearRadioButton = findViewById(R.id.yearRadioButton);
        topProductsRecyclerView = findViewById(R.id.topProductsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        barChart = findViewById(R.id.barChart);
        noDataTxt = findViewById(R.id.noDataTxt);
        noProductsTxt = findViewById(R.id.noProductsTxt);
    }

    // Phương thức thiết lập sự kiện cho RadioGroup
    // Khi người dùng chọn một phạm vi thời gian, cập nhật biến currentTimeRange
    // và tải lại dữ liệu thống kê thu nhập
    private void setupTimeRangeSelection() {
        timeRangeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.todayRadioButton) {
                currentTimeRange = TIME_RANGE_TODAY;
            } else if (checkedId == R.id.weekRadioButton) {
                currentTimeRange = TIME_RANGE_WEEK;
            } else if (checkedId == R.id.monthRadioButton) {
                currentTimeRange = TIME_RANGE_MONTH;
            } else if (checkedId == R.id.yearRadioButton) {
                currentTimeRange = TIME_RANGE_YEAR;
            }
            
            // Tải lại dữ liệu thống kê thu nhập với phạm vi thời gian mới
            loadIncomeStatistics(currentTimeRange);
        });
    }

    // Phương thức thiết lập RecyclerView cho danh sách sản phẩm bán chạy
    private void setupTopProductsRecyclerView() {
        topProductList = new ArrayList<>();
        topProductAdapter = new TopProductAdapter(this, topProductList);
        topProductsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        topProductsRecyclerView.setAdapter(topProductAdapter);
    }

    // Phương thức tải dữ liệu thống kê thu nhập từ Firebase
    private void loadIncomeStatistics(String timeRange) {
        // Hiển thị ProgressBar
        progressBar.setVisibility(View.VISIBLE);
        
        // Reset dữ liệu
        topProductList.clear();
        totalRevenueTxt.setText("0 đ");
        totalOrdersTxt.setText("0");
        noDataTxt.setVisibility(View.GONE);
        noProductsTxt.setVisibility(View.GONE);
        
        // Thêm log để kiểm tra quá trình tải dữ liệu
        Toast.makeText(this, "Đang tải dữ liệu thống kê...", Toast.LENGTH_SHORT).show();
        
        // Truy vấn dữ liệu từ Firebase - không lọc theo timestamp để lấy tất cả đơn hàng
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Tổng doanh thu và số lượng đơn hàng
                    double totalRevenue = 0;
                    int totalOrders = 0;
                    
                    // Map để theo dõi sản phẩm bán chạy (key: productId)
                    Map<String, TopProduct> productMap = new HashMap<>();
                    
                    // Map để lưu doanh thu theo ngày/tuần/tháng cho biểu đồ
                    Map<String, Double> revenueByTime = new HashMap<>();
                    
                    // Lấy timestamp từ thời điểm hiện tại trở về trước dựa vào timeRange
                    long startTimestamp = getStartTimestampFromTimeRange(timeRange);
                    
                    // Duyệt qua tất cả đơn hàng
                    for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                        try {
                            OrderDomain order = orderSnapshot.getValue(OrderDomain.class);
                            
                            if (order != null) {
                                // Hiển thị thông tin về đơn hàng để debug
                                System.out.println("Đơn hàng: ID=" + order.getOrderId() + 
                                        ", Trạng thái=" + order.getStatus() + 
                                        ", Tổng tiền=" + order.getTotalAmount() +
                                        ", Timestamp=" + order.getTimestamp());
                                
                                // Mở rộng điều kiện lọc - chấp nhận tất cả trạng thái đơn hàng
                                // và chỉ lọc theo thời gian
                                // Nếu timestamp = 0 hoặc timestamp > startTimestamp thì tính vào thống kê
                                if (order.getTimestamp() == 0 || order.getTimestamp() >= startTimestamp) {
                                    totalOrders++;
                                    totalRevenue += order.getTotalAmount();
                                    
                                    // Tính doanh thu theo thời gian (ngày, tuần, tháng)
                                    long orderTime = order.getTimestamp();
                                    if (orderTime == 0) {
                                        // Nếu timestamp = 0, sử dụng thời gian hiện tại
                                        orderTime = System.currentTimeMillis();
                                    }
                                    String timeKey = getTimeKey(orderTime, timeRange);
                                    revenueByTime.put(timeKey, revenueByTime.getOrDefault(timeKey, 0.0) + order.getTotalAmount());
                                    
                                    // Thống kê sản phẩm bán chạy
                                    try {
                                        // Thử lấy cartItems dưới dạng Map
                                        Object itemsObj = orderSnapshot.child("items").getValue();
                                        if (itemsObj == null) {
                                            itemsObj = orderSnapshot.child("cartItems").getValue();
                                        }
                                        
                                        if (itemsObj instanceof Map) {
                                            // Trường hợp dữ liệu là Map (như mô hình hiện tại)
                                            Map<String, Object> itemsMap = (Map<String, Object>) itemsObj;
                                            processCartItemsMap(itemsMap, productMap);
                                        } else if (itemsObj instanceof ArrayList) {
                                            // Trường hợp dữ liệu là ArrayList (từ Firebase)
                                            ArrayList<Object> itemsList = (ArrayList<Object>) itemsObj;
                                            processCartItemsList(itemsList, productMap);
                                        } else {
                                            System.out.println("Đơn hàng không có sản phẩm hoặc danh sách sản phẩm có định dạng không xác định: " + 
                                                    (itemsObj != null ? itemsObj.getClass().getName() : "null"));
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Lỗi khi xử lý danh sách sản phẩm: " + e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi xử lý đơn hàng: " + e.getMessage());
                        }
                    }
                    
                    // Cập nhật tổng quan
                    totalRevenueTxt.setText(formatter.format(totalRevenue) + " đ");
                    totalOrdersTxt.setText(String.valueOf(totalOrders));
                    
                    // Thông báo để debug
                    Toast.makeText(IncomeStatisticsActivity.this, 
                            "Đã tìm thấy " + totalOrders + " đơn hàng, tổng doanh thu: " + 
                            formatter.format(totalRevenue) + " đ", Toast.LENGTH_SHORT).show();
                    
                    if (totalOrders > 0) {
                        // Cập nhật danh sách sản phẩm bán chạy
                        List<TopProduct> sortedProducts = new ArrayList<>(productMap.values());
                        if (!sortedProducts.isEmpty()) {
                            Collections.sort(sortedProducts, (p1, p2) -> Double.compare(p2.getRevenue(), p1.getRevenue()));
                            
                            // Giới hạn số lượng sản phẩm hiển thị (top 10)
                            int maxDisplayCount = Math.min(sortedProducts.size(), 10);
                            topProductList.addAll(sortedProducts.subList(0, maxDisplayCount));
                            topProductAdapter.notifyDataSetChanged();
                            
                            // Cập nhật biểu đồ doanh thu
                            updateRevenueChart(revenueByTime, timeRange);
                        } else {
                            noProductsTxt.setVisibility(View.VISIBLE);
                            System.out.println("Không có sản phẩm nào trong danh sách sản phẩm bán chạy");
                        }
                    } else {
                        // Không có dữ liệu
                        noDataTxt.setVisibility(View.VISIBLE);
                        noProductsTxt.setVisibility(View.VISIBLE);
                        System.out.println("Không có đơn hàng nào được tìm thấy trong khoảng thời gian");
                    }
                } else {
                    // Không có dữ liệu
                    noDataTxt.setVisibility(View.VISIBLE);
                    noProductsTxt.setVisibility(View.VISIBLE);
                    Toast.makeText(IncomeStatisticsActivity.this, 
                            "Không tìm thấy dữ liệu đơn hàng trong cơ sở dữ liệu", 
                            Toast.LENGTH_SHORT).show();
                    System.out.println("Không tìm thấy dữ liệu đơn hàng trong Firebase");
                }
                
                // Ẩn ProgressBar
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Xử lý lỗi
                Toast.makeText(IncomeStatisticsActivity.this, 
                        "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                System.err.println("Lỗi Firebase: " + error.getMessage());
            }
        });
    }
    
    // Phương thức để xử lý danh sách sản phẩm dạng Map
    private void processCartItemsMap(Map<String, Object> itemsMap, Map<String, TopProduct> productMap) {
        for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
            try {
                Object itemObj = entry.getValue();
                if (itemObj instanceof Map) {
                    Map<String, Object> itemData = (Map<String, Object>) itemObj;
                    
                    // Lấy dữ liệu sản phẩm từ Map
                    String productId = getStringValue(itemData.get("productId"));
                    if (productId == null) {
                        productId = getStringValue(itemData.get("id"));
                    }
                    
                    String productName = getStringValue(itemData.get("title"));
                    String imageUrl = getStringValue(itemData.get("picUrl"));
                    if (imageUrl == null) {
                        imageUrl = getStringValue(itemData.get("imagePath"));
                    }
                    
                    int quantity = getIntValue(itemData.get("quantity"), 1);
                    double revenue = getDoubleValue(itemData.get("totalPrice"), 0.0);
                    if (revenue == 0.0) {
                        double price = getDoubleValue(itemData.get("price"), 0.0);
                        revenue = price * quantity;
                    }
                    
                    updateProductStatistics(productId, productName, imageUrl, quantity, revenue, productMap);
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý sản phẩm từ Map: " + e.getMessage());
            }
        }
    }
    
    // Phương thức để xử lý danh sách sản phẩm dạng ArrayList
    private void processCartItemsList(ArrayList<Object> itemsList, Map<String, TopProduct> productMap) {
        for (Object itemObj : itemsList) {
            try {
                if (itemObj instanceof Map) {
                    Map<String, Object> itemData = (Map<String, Object>) itemObj;
                    
                    // Lấy dữ liệu sản phẩm từ Map trong ArrayList
                    String productId = getStringValue(itemData.get("productId"));
                    if (productId == null) {
                        productId = getStringValue(itemData.get("id"));
                    }
                    
                    String productName = getStringValue(itemData.get("title"));
                    String imageUrl = getStringValue(itemData.get("picUrl"));
                    if (imageUrl == null) {
                        imageUrl = getStringValue(itemData.get("imagePath"));
                    }
                    
                    int quantity = getIntValue(itemData.get("quantity"), 1);
                    double revenue = getDoubleValue(itemData.get("totalPrice"), 0.0);
                    if (revenue == 0.0) {
                        double price = getDoubleValue(itemData.get("price"), 0.0);
                        revenue = price * quantity;
                    }
                    
                    updateProductStatistics(productId, productName, imageUrl, quantity, revenue, productMap);
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý sản phẩm từ ArrayList: " + e.getMessage());
            }
        }
    }
    
    // Phương thức cập nhật thống kê sản phẩm
    private void updateProductStatistics(String productId, String productName, String imageUrl, int quantity, double revenue, Map<String, TopProduct> productMap) {
        if (productId != null && !productId.isEmpty()) {
            if (productMap.containsKey(productId)) {
                TopProduct existingProduct = productMap.get(productId);
                existingProduct.setQuantitySold(existingProduct.getQuantitySold() + quantity);
                existingProduct.setRevenue(existingProduct.getRevenue() + revenue);
            } else {
                TopProduct newProduct = new TopProduct(productId, productName, imageUrl, quantity, revenue);
                productMap.put(productId, newProduct);
            }
        }
    }
    
    // Phương thức hỗ trợ lấy giá trị String từ Object
    private String getStringValue(Object value) {
        if (value == null) return null;
        return String.valueOf(value);
    }
    
    // Phương thức hỗ trợ lấy giá trị int từ Object
    private int getIntValue(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            } else if (value instanceof Double) {
                return ((Double) value).intValue();
            }
        } catch (Exception e) {
            System.err.println("Lỗi chuyển đổi giá trị sang int: " + e.getMessage());
        }
        return defaultValue;
    }
    
    // Phương thức hỗ trợ lấy giá trị double từ Object
    private double getDoubleValue(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Integer) {
                return ((Integer) value).doubleValue();
            } else if (value instanceof Long) {
                return ((Long) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        } catch (Exception e) {
            System.err.println("Lỗi chuyển đổi giá trị sang double: " + e.getMessage());
        }
        return defaultValue;
    }
    
    private long getStartTimestampFromTimeRange(String timeRange) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        switch (timeRange) {
            case TIME_RANGE_TODAY:
                // Thời gian bắt đầu của ngày hôm nay
                break;
            case TIME_RANGE_WEEK:
                // Thời gian bắt đầu của tuần này (Chủ Nhật)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                break;
            case TIME_RANGE_MONTH:
                // Thời gian bắt đầu của tháng này
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case TIME_RANGE_YEAR:
                // Thời gian bắt đầu của năm này
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                break;
        }
        
        return calendar.getTimeInMillis();
    }
    
    private String getTimeKey(long timestamp, String timeRange) {
        SimpleDateFormat sdf;
        
        switch (timeRange) {
            case TIME_RANGE_TODAY:
                // Theo giờ
                sdf = new SimpleDateFormat("HH:00", Locale.getDefault());
                break;
            case TIME_RANGE_WEEK:
                // Theo ngày trong tuần
                sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                break;
            case TIME_RANGE_MONTH:
                // Theo ngày trong tháng
                sdf = new SimpleDateFormat("dd", Locale.getDefault());
                break;
            case TIME_RANGE_YEAR:
                // Theo tháng trong năm
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                break;
            default:
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                break;
        }
        
        return sdf.format(new Date(timestamp));
    }
    
    private void updateRevenueChart(Map<String, Double> revenueData, String timeRange) {
        if (revenueData.isEmpty()) {
            noDataTxt.setVisibility(View.VISIBLE);
            return;
        }
        
        // Tạo entries cho biểu đồ
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Tạo dữ liệu biểu đồ dựa trên loại thời gian
        List<String> timeKeys = getOrderedTimeKeys(timeRange);
        
        for (int i = 0; i < timeKeys.size(); i++) {
            String key = timeKeys.get(i);
            double value = revenueData.getOrDefault(key, 0.0);
            entries.add(new BarEntry(i, (float) value));
            labels.add(key);
        }
        
        // Tạo dataset
        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(getResources().getColor(R.color.white));
        dataSet.setValueTextSize(10f);
        
        // Tạo data và thiết lập cho biểu đồ
        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        // Định dạng trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.white));
        
        // Định dạng trục Y
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.white));
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Cấu hình khác
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.getLegend().setTextColor(getResources().getColor(R.color.white));
        
        // Cập nhật biểu đồ
        barChart.invalidate();
    }
    
    private List<String> getOrderedTimeKeys(String timeRange) {
        List<String> keys = new ArrayList<>();
        SimpleDateFormat sdf;
        Calendar calendar = Calendar.getInstance();
        
        switch (timeRange) {
            case TIME_RANGE_TODAY:
                // 24 giờ trong ngày
                sdf = new SimpleDateFormat("HH:00", Locale.getDefault());
                for (int i = 0; i < 24; i++) {
                    calendar.set(Calendar.HOUR_OF_DAY, i);
                    calendar.set(Calendar.MINUTE, 0);
                    keys.add(sdf.format(calendar.getTime()));
                }
                break;
                
            case TIME_RANGE_WEEK:
                // Các ngày trong tuần
                sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                for (int i = 0; i < 7; i++) {
                    keys.add(sdf.format(calendar.getTime()));
                    calendar.add(Calendar.DAY_OF_WEEK, 1);
                }
                break;
                
            case TIME_RANGE_MONTH:
                // Các ngày trong tháng
                int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (int i = 1; i <= maxDay; i++) {
                    keys.add(String.valueOf(i));
                }
                break;
                
            case TIME_RANGE_YEAR:
                // Các tháng trong năm
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                for (int i = 0; i < 12; i++) {
                    keys.add(sdf.format(calendar.getTime()));
                    calendar.add(Calendar.MONTH, 1);
                }
                break;
        }
        
        return keys;
    }
}
