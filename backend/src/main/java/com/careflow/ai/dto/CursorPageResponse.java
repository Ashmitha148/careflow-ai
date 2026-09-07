package com.careflow.ai.dto;

import java.util.List;

public class CursorPageResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;
    private int pageSize;

    public CursorPageResponse() {}

    public CursorPageResponse(List<T> items, String nextCursor, boolean hasMore, int pageSize) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public static <T> CursorPageResponseBuilder<T> builder() {
        return new CursorPageResponseBuilder<>();
    }

    public static class CursorPageResponseBuilder<T> {
        private List<T> items;
        private String nextCursor;
        private boolean hasMore;
        private int pageSize;

        public CursorPageResponseBuilder<T> items(List<T> items) { this.items = items; return this; }
        public CursorPageResponseBuilder<T> nextCursor(String nextCursor) { this.nextCursor = nextCursor; return this; }
        public CursorPageResponseBuilder<T> hasMore(boolean hasMore) { this.hasMore = hasMore; return this; }
        public CursorPageResponseBuilder<T> pageSize(int pageSize) { this.pageSize = pageSize; return this; }

        public CursorPageResponse<T> build() {
            return new CursorPageResponse<>(items, nextCursor, hasMore, pageSize);
        }
    }
}
