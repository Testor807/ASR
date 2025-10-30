package com.example.cantonesevoicerecognition.ui.history;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cantonesevoicerecognition.R;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;
import com.example.cantonesevoicerecognition.data.repository.RepositoryCallback;
import com.example.cantonesevoicerecognition.data.repository.TranscriptionRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * 转录历史记录Fragment
 */
public class HistoryFragment extends Fragment implements 
    TranscriptionHistoryAdapter.OnItemClickListener,
    TranscriptionHistoryAdapter.OnItemActionListener {
    
    private static final String TAG = "HistoryFragment";
    
    // UI组件
    private TextInputEditText etSearch;
    private MaterialButton btnClearSearch;
    private MaterialButton btnFilter;
    private MaterialButton btnSort;
    private TextView tvRecordCount;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvHistory;
    private LinearLayout llEmptyState;
    private FloatingActionButton fabDeleteAll;
    
    // 数据和适配器
    private TranscriptionRepository repository;
    private TranscriptionHistoryAdapter adapter;
    
    // 状态管理
    private String currentSearchQuery = "";
    private SortType currentSortType = SortType.TIME_DESC;
    private FilterType currentFilterType = FilterType.ALL;
    
    public enum SortType {
        TIME_ASC, TIME_DESC, CONFIDENCE_ASC, CONFIDENCE_DESC, LENGTH_ASC, LENGTH_DESC
    }
    
    public enum FilterType {
        ALL, REAL_TIME_ONLY, HIGH_CONFIDENCE_ONLY
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        initializeRepository();
        initializeAdapter();
        setupListeners();
        loadHistoryData();
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeViews(View view) {
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        btnFilter = view.findViewById(R.id.btn_filter);
        btnSort = view.findViewById(R.id.btn_sort);
        tvRecordCount = view.findViewById(R.id.tv_record_count);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        rvHistory = view.findViewById(R.id.rv_history);
        llEmptyState = view.findViewById(R.id.ll_empty_state);
        fabDeleteAll = view.findViewById(R.id.fab_delete_all);
    }
    
    /**
     * 初始化数据仓库
     */
    private void initializeRepository() {
        repository = new TranscriptionRepository(requireActivity().getApplication());
    }
    
    /**
     * 初始化适配器
     */
    private void initializeAdapter() {
        adapter = new TranscriptionHistoryAdapter(requireContext());
        adapter.setOnItemClickListener(this);
        adapter.setOnItemActionListener(this);
        
        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHistory.setAdapter(adapter);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 搜索框监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                adapter.filter(currentSearchQuery);
                updateRecordCount();
                updateClearSearchButton();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 清除搜索按钮
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            currentSearchQuery = "";
        });
        
        // 筛选按钮
        btnFilter.setOnClickListener(v -> showFilterDialog());
        
        // 排序按钮
        btnSort.setOnClickListener(v -> showSortDialog());
        
        // 下拉刷新
        swipeRefresh.setOnRefreshListener(this::loadHistoryData);
        
        // 删除所有按钮
        fabDeleteAll.setOnClickListener(v -> showDeleteAllDialog());
    }
    
    /**
     * 加载历史数据
     */
    private void loadHistoryData() {
        swipeRefresh.setRefreshing(true);
        
        repository.getAllTranscriptions(new RepositoryCallback<List<TranscriptionRecord>>() {
            @Override
            public void onSuccess(List<TranscriptionRecord> result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        adapter.updateData(result);
                        applySorting();
                        applyFiltering();
                        updateUI();
                        swipeRefresh.setRefreshing(false);
                    });
                }
            }
            
            @Override
            public void onError(Exception error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "加载历史记录失败: " + error.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    });
                }
            }
        });
    }
    
    /**
     * 应用排序
     */
    private void applySorting() {
        switch (currentSortType) {
            case TIME_ASC:
                adapter.sortByTime(true);
                break;
            case TIME_DESC:
                adapter.sortByTime(false);
                break;
            case CONFIDENCE_ASC:
                adapter.sortByConfidence(true);
                break;
            case CONFIDENCE_DESC:
                adapter.sortByConfidence(false);
                break;
            case LENGTH_ASC:
                adapter.sortByLength(true);
                break;
            case LENGTH_DESC:
                adapter.sortByLength(false);
                break;
        }
    }
    
    /**
     * 应用筛选
     */
    private void applyFiltering() {
        switch (currentFilterType) {
            case REAL_TIME_ONLY:
                adapter.filterByRealTime(true);
                break;
            case HIGH_CONFIDENCE_ONLY:
                adapter.filterByHighConfidence(0.8f);
                break;
            case ALL:
            default:
                // 不需要额外筛选
                break;
        }
    }
    
    /**
     * 更新UI状态
     */
    private void updateUI() {
        updateRecordCount();
        updateEmptyState();
        updateDeleteAllButton();
    }
    
    /**
     * 更新记录数量显示
     */
    private void updateRecordCount() {
        int filteredCount = adapter.getFilteredCount();
        int totalCount = adapter.getTotalCount();
        
        if (filteredCount == totalCount) {
            tvRecordCount.setText("共 " + totalCount + " 条记录");
        } else {
            tvRecordCount.setText("显示 " + filteredCount + " / " + totalCount + " 条记录");
        }
    }
    
    /**
     * 更新空状态显示
     */
    private void updateEmptyState() {
        boolean isEmpty = adapter.getFilteredCount() == 0;
        llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
    
    /**
     * 更新清除搜索按钮
     */
    private void updateClearSearchButton() {
        btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
    }
    
    /**
     * 更新删除所有按钮
     */
    private void updateDeleteAllButton() {
        boolean hasRecords = adapter.getTotalCount() > 0;
        fabDeleteAll.setVisibility(hasRecords ? View.VISIBLE : View.GONE);
    }
    
    /**
     * 显示筛选对话框
     */
    private void showFilterDialog() {
        String[] filterOptions = {"全部记录", "仅实时转录", "仅高置信度"};
        int currentSelection = currentFilterType.ordinal();
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("筛选记录")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    currentFilterType = FilterType.values()[which];
                    applyFiltering();
                    updateUI();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示排序对话框
     */
    private void showSortDialog() {
        String[] sortOptions = {
            "时间 (最新优先)", "时间 (最旧优先)",
            "置信度 (高到低)", "置信度 (低到高)",
            "长度 (长到短)", "长度 (短到长)"
        };
        int currentSelection = currentSortType.ordinal();
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("排序方式")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    currentSortType = SortType.values()[which];
                    applySorting();
                    updateUI();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示删除所有对话框
     */
    private void showDeleteAllDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除所有记录")
                .setMessage("确定要删除所有转录记录吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> deleteAllRecords())
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除所有记录
     */
    private void deleteAllRecords() {
        // TODO: 实现批量删除功能
        Toast.makeText(requireContext(), "批量删除功能开发中", Toast.LENGTH_SHORT).show();
    }
    
    // TranscriptionHistoryAdapter.OnItemClickListener 实现
    
    @Override
    public void onItemClick(TranscriptionRecord record, int position) {
        showRecordDetailDialog(record);
    }
    
    @Override
    public void onItemLongClick(TranscriptionRecord record, int position) {
        // 长按选择多个项目（可选功能）
        Toast.makeText(requireContext(), "长按功能开发中", Toast.LENGTH_SHORT).show();
    }
    
    // TranscriptionHistoryAdapter.OnItemActionListener 实现
    
    @Override
    public void onEdit(TranscriptionRecord record, int position) {
        showEditDialog(record, position);
    }
    
    @Override
    public void onDelete(TranscriptionRecord record, int position) {
        showDeleteDialog(record, position);
    }
    
    @Override
    public void onShare(TranscriptionRecord record, int position) {
        shareRecord(record);
    }
    
    @Override
    public void onCopy(TranscriptionRecord record, int position) {
        copyToClipboard(record);
    }
    
    @Override
    public void onPlayAudio(TranscriptionRecord record, int position) {
        playAudio(record);
    }
    
    /**
     * 显示记录详情对话框
     */
    private void showRecordDetailDialog(TranscriptionRecord record) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_transcription_detail, null);
        
        // TODO: 设置详情对话框内容
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("转录详情")
                .setView(dialogView)
                .setPositiveButton("关闭", null)
                .show();
    }
    
    /**
     * 显示编辑对话框
     */
    private void showEditDialog(TranscriptionRecord record, int position) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_transcription, null);
        
        TextInputEditText etEditText = dialogView.findViewById(R.id.et_edit_text);
        String currentText = record.getEditedText() != null ? 
                           record.getEditedText() : record.getOriginalText();
        etEditText.setText(currentText);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("编辑转录文本")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newText = etEditText.getText().toString().trim();
                    if (!newText.isEmpty()) {
                        updateRecord(record, newText, position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(TranscriptionRecord record, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除记录")
                .setMessage("确定要删除这条转录记录吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteRecord(record, position))
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 更新记录
     */
    private void updateRecord(TranscriptionRecord record, String newText, int position) {
        record.setEditedText(newText);
        
        // TODO: 保存到数据库
        adapter.updateRecord(position, record);
        Toast.makeText(requireContext(), "记录已更新", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 删除记录
     */
    private void deleteRecord(TranscriptionRecord record, int position) {
        // TODO: 从数据库删除
        adapter.removeRecord(position);
        updateUI();
        Toast.makeText(requireContext(), "记录已删除", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 分享记录
     */
    private void shareRecord(TranscriptionRecord record) {
        String shareText = record.getEditedText() != null ? 
                         record.getEditedText() : record.getOriginalText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "转录文本分享");
        
        startActivity(Intent.createChooser(shareIntent, "分享转录文本"));
    }
    
    /**
     * 复制到剪贴板
     */
    private void copyToClipboard(TranscriptionRecord record) {
        String copyText = record.getEditedText() != null ? 
                        record.getEditedText() : record.getOriginalText();
        
        ClipboardManager clipboard = (ClipboardManager) 
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("转录文本", copyText);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 播放音频
     */
    private void playAudio(TranscriptionRecord record) {
        if (record.hasAudioFile()) {
            // TODO: 实现音频播放功能
            Toast.makeText(requireContext(), "音频播放功能开发中", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "该记录没有关联的音频文件", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 刷新数据以获取最新的转录记录
        loadHistoryData();
    }
}