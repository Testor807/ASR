package com.example.cantonesevoicerecognition.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cantonesevoicerecognition.R;
import com.example.cantonesevoicerecognition.data.model.TranscriptionRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 转录历史记录适配器
 */
public class TranscriptionHistoryAdapter extends RecyclerView.Adapter<TranscriptionHistoryAdapter.ViewHolder> {
    
    private Context context;
    private List<TranscriptionRecord> records;
    private List<TranscriptionRecord> filteredRecords;
    private OnItemClickListener onItemClickListener;
    private OnItemActionListener onItemActionListener;
    private SimpleDateFormat dateFormat;
    
    public interface OnItemClickListener {
        void onItemClick(TranscriptionRecord record, int position);
        void onItemLongClick(TranscriptionRecord record, int position);
    }
    
    public interface OnItemActionListener {
        void onEdit(TranscriptionRecord record, int position);
        void onDelete(TranscriptionRecord record, int position);
        void onShare(TranscriptionRecord record, int position);
        void onCopy(TranscriptionRecord record, int position);
        void onPlayAudio(TranscriptionRecord record, int position);
    }
    
    public TranscriptionHistoryAdapter(Context context) {
        this.context = context;
        this.records = new ArrayList<>();
        this.filteredRecords = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transcription_record, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TranscriptionRecord record = filteredRecords.get(position);
        holder.bind(record, position);
    }
    
    @Override
    public int getItemCount() {
        return filteredRecords.size();
    }
    
    /**
     * 更新数据
     */
    public void updateData(List<TranscriptionRecord> newRecords) {
        this.records.clear();
        this.records.addAll(newRecords);
        this.filteredRecords.clear();
        this.filteredRecords.addAll(newRecords);
        notifyDataSetChanged();
    }
    
    /**
     * 搜索过滤
     */
    public void filter(String query) {
        filteredRecords.clear();
        
        if (query == null || query.trim().isEmpty()) {
            filteredRecords.addAll(records);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (TranscriptionRecord record : records) {
                if (record.getOriginalText().toLowerCase().contains(lowerQuery) ||
                    (record.getEditedText() != null && record.getEditedText().toLowerCase().contains(lowerQuery))) {
                    filteredRecords.add(record);
                }
            }
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * 按时间排序
     */
    public void sortByTime(boolean ascending) {
        filteredRecords.sort((r1, r2) -> {
            if (ascending) {
                return Long.compare(r1.getTimestamp(), r2.getTimestamp());
            } else {
                return Long.compare(r2.getTimestamp(), r1.getTimestamp());
            }
        });
        notifyDataSetChanged();
    }
    
    /**
     * 按置信度排序
     */
    public void sortByConfidence(boolean ascending) {
        filteredRecords.sort((r1, r2) -> {
            if (ascending) {
                return Float.compare(r1.getConfidence(), r2.getConfidence());
            } else {
                return Float.compare(r2.getConfidence(), r1.getConfidence());
            }
        });
        notifyDataSetChanged();
    }
    
    /**
     * 按文本长度排序
     */
    public void sortByLength(boolean ascending) {
        filteredRecords.sort((r1, r2) -> {
            int len1 = r1.getOriginalText().length();
            int len2 = r2.getOriginalText().length();
            if (ascending) {
                return Integer.compare(len1, len2);
            } else {
                return Integer.compare(len2, len1);
            }
        });
        notifyDataSetChanged();
    }
    
    /**
     * 筛选实时转录记录
     */
    public void filterByRealTime(boolean realTimeOnly) {
        filteredRecords.clear();
        for (TranscriptionRecord record : records) {
            if (!realTimeOnly || record.isRealTime()) {
                filteredRecords.add(record);
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 筛选高置信度记录
     */
    public void filterByHighConfidence(float threshold) {
        filteredRecords.clear();
        for (TranscriptionRecord record : records) {
            if (record.getConfidence() >= threshold) {
                filteredRecords.add(record);
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 获取当前显示的记录数量
     */
    public int getFilteredCount() {
        return filteredRecords.size();
    }
    
    /**
     * 获取总记录数量
     */
    public int getTotalCount() {
        return records.size();
    }
    
    /**
     * 删除记录
     */
    public void removeRecord(int position) {
        if (position >= 0 && position < filteredRecords.size()) {
            TranscriptionRecord record = filteredRecords.get(position);
            records.remove(record);
            filteredRecords.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    /**
     * 更新记录
     */
    public void updateRecord(int position, TranscriptionRecord updatedRecord) {
        if (position >= 0 && position < filteredRecords.size()) {
            filteredRecords.set(position, updatedRecord);
            // 同时更新原始列表
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).getId() == updatedRecord.getId()) {
                    records.set(i, updatedRecord);
                    break;
                }
            }
            notifyItemChanged(position);
        }
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    public void setOnItemActionListener(OnItemActionListener listener) {
        this.onItemActionListener = listener;
    }
    
    /**
     * ViewHolder类
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView ivTypeIcon;
        private TextView tvTimestamp;
        private TextView tvConfidence;
        private View viewConfidenceIndicator;
        private ImageButton btnMore;
        private TextView tvTranscriptionText;
        private TextView tvDuration;
        private TextView tvWordCount;
        private TextView tvEditedIndicator;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            ivTypeIcon = itemView.findViewById(R.id.iv_type_icon);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            viewConfidenceIndicator = itemView.findViewById(R.id.view_confidence_indicator);
            btnMore = itemView.findViewById(R.id.btn_more);
            tvTranscriptionText = itemView.findViewById(R.id.tv_transcription_text);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvWordCount = itemView.findViewById(R.id.tv_word_count);
            tvEditedIndicator = itemView.findViewById(R.id.tv_edited_indicator);
            
            // 设置点击监听器
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(filteredRecords.get(position), position);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemLongClick(filteredRecords.get(position), position);
                    return true;
                }
                return false;
            });
            
            btnMore.setOnClickListener(v -> showPopupMenu(v, getAdapterPosition()));
        }
        
        public void bind(TranscriptionRecord record, int position) {
            // 设置类型图标
            if (record.isRealTime()) {
                ivTypeIcon.setImageResource(R.drawable.ic_realtime);
            } else {
                ivTypeIcon.setImageResource(R.drawable.ic_file);
            }
            
            // 设置时间戳
            tvTimestamp.setText(dateFormat.format(new Date(record.getTimestamp())));
            
            // 设置置信度
            int confidencePercent = Math.round(record.getConfidence() * 100);
            tvConfidence.setText(confidencePercent + "%");
            
            // 设置置信度指示器颜色
            if (record.getConfidence() >= 0.8f) {
                viewConfidenceIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.success_color));
            } else if (record.getConfidence() >= 0.6f) {
                viewConfidenceIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.warning_color));
            } else {
                viewConfidenceIndicator.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.error_color));
            }
            
            // 设置转录文本
            String displayText = record.getEditedText() != null && !record.getEditedText().isEmpty() 
                ? record.getEditedText() : record.getOriginalText();
            tvTranscriptionText.setText(displayText);
            
            // 设置时长
            tvDuration.setText(record.getFormattedDuration());
            
            // 设置字数
            int wordCount = displayText.length();
            tvWordCount.setText(wordCount + "字");
            
            // 设置编辑指示器
            boolean isEdited = record.getEditedText() != null && 
                !record.getEditedText().equals(record.getOriginalText());
            tvEditedIndicator.setVisibility(isEdited ? View.VISIBLE : View.GONE);
        }
        
        private void showPopupMenu(View anchor, int position) {
            if (position == RecyclerView.NO_POSITION || onItemActionListener == null) {
                return;
            }
            
            TranscriptionRecord record = filteredRecords.get(position);
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenuInflater().inflate(R.menu.menu_transcription_item, popup.getMenu());
            
            // 根据记录状态调整菜单项
            if (!record.hasAudioFile()) {
                popup.getMenu().findItem(R.id.action_play_audio).setVisible(false);
            }
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit) {
                    onItemActionListener.onEdit(record, position);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    onItemActionListener.onDelete(record, position);
                    return true;
                } else if (itemId == R.id.action_share) {
                    onItemActionListener.onShare(record, position);
                    return true;
                } else if (itemId == R.id.action_copy) {
                    onItemActionListener.onCopy(record, position);
                    return true;
                } else if (itemId == R.id.action_play_audio) {
                    onItemActionListener.onPlayAudio(record, position);
                    return true;
                }
                return false;
            });
            
            popup.show();
        }
    }
}