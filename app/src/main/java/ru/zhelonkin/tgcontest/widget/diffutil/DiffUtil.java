package ru.zhelonkin.tgcontest.widget.diffutil;//
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DiffUtil {
    private static final Comparator<DiffUtil.Snake> SNAKE_COMPARATOR = new Comparator<DiffUtil.Snake>() {
        public int compare(DiffUtil.Snake o1, DiffUtil.Snake o2) {
            int cmpX = o1.x - o2.x;
            return cmpX == 0 ? o1.y - o2.y : cmpX;
        }
    };

    private DiffUtil() {
    }

    @NonNull
    public static DiffUtil.DiffResult calculateDiff(@NonNull DiffUtil.Callback cb) {
        return calculateDiff(cb, true);
    }

    @NonNull
    public static DiffUtil.DiffResult calculateDiff(@NonNull DiffUtil.Callback cb, boolean detectMoves) {
        int oldSize = cb.getOldListSize();
        int newSize = cb.getNewListSize();
        List<DiffUtil.Snake> snakes = new ArrayList();
        List<DiffUtil.Range> stack = new ArrayList();
        stack.add(new DiffUtil.Range(0, oldSize, 0, newSize));
        int max = oldSize + newSize + Math.abs(oldSize - newSize);
        int[] forward = new int[max * 2];
        int[] backward = new int[max * 2];
        ArrayList rangePool = new ArrayList();

        while(!stack.isEmpty()) {
            DiffUtil.Range range = (DiffUtil.Range)stack.remove(stack.size() - 1);
            DiffUtil.Snake snake = diffPartial(cb, range.oldListStart, range.oldListEnd, range.newListStart, range.newListEnd, forward, backward, max);
            if (snake != null) {
                if (snake.size > 0) {
                    snakes.add(snake);
                }

                snake.x += range.oldListStart;
                snake.y += range.newListStart;
                DiffUtil.Range left = rangePool.isEmpty() ? new DiffUtil.Range() : (DiffUtil.Range)rangePool.remove(rangePool.size() - 1);
                left.oldListStart = range.oldListStart;
                left.newListStart = range.newListStart;
                if (snake.reverse) {
                    left.oldListEnd = snake.x;
                    left.newListEnd = snake.y;
                } else if (snake.removal) {
                    left.oldListEnd = snake.x - 1;
                    left.newListEnd = snake.y;
                } else {
                    left.oldListEnd = snake.x;
                    left.newListEnd = snake.y - 1;
                }

                stack.add(left);
                if (snake.reverse) {
                    if (snake.removal) {
                        range.oldListStart = snake.x + snake.size + 1;
                        range.newListStart = snake.y + snake.size;
                    } else {
                        range.oldListStart = snake.x + snake.size;
                        range.newListStart = snake.y + snake.size + 1;
                    }
                } else {
                    range.oldListStart = snake.x + snake.size;
                    range.newListStart = snake.y + snake.size;
                }

                stack.add(range);
            } else {
                rangePool.add(range);
            }
        }

        Collections.sort(snakes, SNAKE_COMPARATOR);
        return new DiffUtil.DiffResult(cb, snakes, forward, backward, detectMoves);
    }

    private static DiffUtil.Snake diffPartial(DiffUtil.Callback cb, int startOld, int endOld, int startNew, int endNew, int[] forward, int[] backward, int kOffset) {
        int oldSize = endOld - startOld;
        int newSize = endNew - startNew;
        if (endOld - startOld >= 1 && endNew - startNew >= 1) {
            int delta = oldSize - newSize;
            int dLimit = (oldSize + newSize + 1) / 2;
            Arrays.fill(forward, kOffset - dLimit - 1, kOffset + dLimit + 1, 0);
            Arrays.fill(backward, kOffset - dLimit - 1 + delta, kOffset + dLimit + 1 + delta, oldSize);
            boolean checkInFwd = delta % 2 != 0;

            for(int d = 0; d <= dLimit; ++d) {
                int k;
                int backwardK;
                for(k = -d; k <= d; k += 2) {
                    boolean removal;
                    if (k != -d && (k == d || forward[kOffset + k - 1] >= forward[kOffset + k + 1])) {
                        backwardK = forward[kOffset + k - 1] + 1;
                        removal = true;
                    } else {
                        backwardK = forward[kOffset + k + 1];
                        removal = false;
                    }

                    for(int y = backwardK - k; backwardK < oldSize && y < newSize && cb.areItemsTheSame(startOld + backwardK, startNew + y); ++y) {
                        ++backwardK;
                    }

                    forward[kOffset + k] = backwardK;
                    if (checkInFwd && k >= delta - d + 1 && k <= delta + d - 1 && forward[kOffset + k] >= backward[kOffset + k]) {
                        DiffUtil.Snake outSnake = new DiffUtil.Snake();
                        outSnake.x = backward[kOffset + k];
                        outSnake.y = outSnake.x - k;
                        outSnake.size = forward[kOffset + k] - backward[kOffset + k];
                        outSnake.removal = removal;
                        outSnake.reverse = false;
                        return outSnake;
                    }
                }

                for(k = -d; k <= d; k += 2) {
                    backwardK = k + delta;
                    int x;
                    boolean removal;
                    if (backwardK != d + delta && (backwardK == -d + delta || backward[kOffset + backwardK - 1] >= backward[kOffset + backwardK + 1])) {
                        x = backward[kOffset + backwardK + 1] - 1;
                        removal = true;
                    } else {
                        x = backward[kOffset + backwardK - 1];
                        removal = false;
                    }

                    for(int y = x - backwardK; x > 0 && y > 0 && cb.areItemsTheSame(startOld + x - 1, startNew + y - 1); --y) {
                        --x;
                    }

                    backward[kOffset + backwardK] = x;
                    if (!checkInFwd && k + delta >= -d && k + delta <= d && forward[kOffset + backwardK] >= backward[kOffset + backwardK]) {
                        DiffUtil.Snake outSnake = new DiffUtil.Snake();
                        outSnake.x = backward[kOffset + backwardK];
                        outSnake.y = outSnake.x - backwardK;
                        outSnake.size = forward[kOffset + backwardK] - backward[kOffset + backwardK];
                        outSnake.removal = removal;
                        outSnake.reverse = true;
                        return outSnake;
                    }
                }
            }

            throw new IllegalStateException("DiffUtil hit an unexpected case while trying to calculate the optimal path. Please make sure your data is not changing during the diff calculation.");
        } else {
            return null;
        }
    }

    private static class PostponedUpdate {
        int posInOwnerList;
        int currentPos;
        boolean removal;

        public PostponedUpdate(int posInOwnerList, int currentPos, boolean removal) {
            this.posInOwnerList = posInOwnerList;
            this.currentPos = currentPos;
            this.removal = removal;
        }
    }

    public static class DiffResult {
        public static final int NO_POSITION = -1;
        private static final int FLAG_NOT_CHANGED = 1;
        private static final int FLAG_CHANGED = 2;
        private static final int FLAG_MOVED_CHANGED = 4;
        private static final int FLAG_MOVED_NOT_CHANGED = 8;
        private static final int FLAG_IGNORE = 16;
        private static final int FLAG_OFFSET = 5;
        private static final int FLAG_MASK = 31;
        private final List<DiffUtil.Snake> mSnakes;
        private final int[] mOldItemStatuses;
        private final int[] mNewItemStatuses;
        private final DiffUtil.Callback mCallback;
        private final int mOldListSize;
        private final int mNewListSize;
        private final boolean mDetectMoves;

        DiffResult(DiffUtil.Callback callback, List<DiffUtil.Snake> snakes, int[] oldItemStatuses, int[] newItemStatuses, boolean detectMoves) {
            this.mSnakes = snakes;
            this.mOldItemStatuses = oldItemStatuses;
            this.mNewItemStatuses = newItemStatuses;
            Arrays.fill(this.mOldItemStatuses, 0);
            Arrays.fill(this.mNewItemStatuses, 0);
            this.mCallback = callback;
            this.mOldListSize = callback.getOldListSize();
            this.mNewListSize = callback.getNewListSize();
            this.mDetectMoves = detectMoves;
            this.addRootSnake();
            this.findMatchingItems();
        }

        private void addRootSnake() {
            DiffUtil.Snake firstSnake = this.mSnakes.isEmpty() ? null : (DiffUtil.Snake)this.mSnakes.get(0);
            if (firstSnake == null || firstSnake.x != 0 || firstSnake.y != 0) {
                DiffUtil.Snake root = new DiffUtil.Snake();
                root.x = 0;
                root.y = 0;
                root.removal = false;
                root.size = 0;
                root.reverse = false;
                this.mSnakes.add(0, root);
            }

        }

        private void findMatchingItems() {
            int posOld = this.mOldListSize;
            int posNew = this.mNewListSize;

            for(int i = this.mSnakes.size() - 1; i >= 0; --i) {
                DiffUtil.Snake snake = (DiffUtil.Snake)this.mSnakes.get(i);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                if (this.mDetectMoves) {
                    while(posOld > endX) {
                        this.findAddition(posOld, posNew, i);
                        --posOld;
                    }

                    while(posNew > endY) {
                        this.findRemoval(posOld, posNew, i);
                        --posNew;
                    }
                }

                for(int j = 0; j < snake.size; ++j) {
                    int oldItemPos = snake.x + j;
                    int newItemPos = snake.y + j;
                    boolean theSame = this.mCallback.areContentsTheSame(oldItemPos, newItemPos);
                    int changeFlag = theSame ? 1 : 2;
                    this.mOldItemStatuses[oldItemPos] = newItemPos << 5 | changeFlag;
                    this.mNewItemStatuses[newItemPos] = oldItemPos << 5 | changeFlag;
                }

                posOld = snake.x;
                posNew = snake.y;
            }

        }

        private void findAddition(int x, int y, int snakeIndex) {
            if (this.mOldItemStatuses[x - 1] == 0) {
                this.findMatchingItem(x, y, snakeIndex, false);
            }
        }

        private void findRemoval(int x, int y, int snakeIndex) {
            if (this.mNewItemStatuses[y - 1] == 0) {
                this.findMatchingItem(x, y, snakeIndex, true);
            }
        }

        public int convertOldPositionToNew(@IntRange(from = 0L) int oldListPosition) {
            if (oldListPosition >= 0 && oldListPosition < this.mOldItemStatuses.length) {
                int status = this.mOldItemStatuses[oldListPosition];
                return (status & 31) == 0 ? -1 : status >> 5;
            } else {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = " + oldListPosition + ", old list size = " + this.mOldItemStatuses.length);
            }
        }

        public int convertNewPositionToOld(@IntRange(from = 0L) int newListPosition) {
            if (newListPosition >= 0 && newListPosition < this.mNewItemStatuses.length) {
                int status = this.mNewItemStatuses[newListPosition];
                return (status & 31) == 0 ? -1 : status >> 5;
            } else {
                throw new IndexOutOfBoundsException("Index out of bounds - passed position = " + newListPosition + ", new list size = " + this.mNewItemStatuses.length);
            }
        }

        private boolean findMatchingItem(int x, int y, int snakeIndex, boolean removal) {
            int myItemPos;
            int curX;
            int curY;
            if (removal) {
                myItemPos = y - 1;
                curX = x;
                curY = y - 1;
            } else {
                myItemPos = x - 1;
                curX = x - 1;
                curY = y;
            }

            for(int i = snakeIndex; i >= 0; --i) {
                DiffUtil.Snake snake = (DiffUtil.Snake)this.mSnakes.get(i);
                int endX = snake.x + snake.size;
                int endY = snake.y + snake.size;
                int pos;
                boolean theSame;
                int changeFlag;
                if (removal) {
                    for(pos = curX - 1; pos >= endX; --pos) {
                        if (this.mCallback.areItemsTheSame(pos, myItemPos)) {
                            theSame = this.mCallback.areContentsTheSame(pos, myItemPos);
                            changeFlag = theSame ? 8 : 4;
                            this.mNewItemStatuses[myItemPos] = pos << 5 | 16;
                            this.mOldItemStatuses[pos] = myItemPos << 5 | changeFlag;
                            return true;
                        }
                    }
                } else {
                    for(pos = curY - 1; pos >= endY; --pos) {
                        if (this.mCallback.areItemsTheSame(myItemPos, pos)) {
                            theSame = this.mCallback.areContentsTheSame(myItemPos, pos);
                            changeFlag = theSame ? 8 : 4;
                            this.mOldItemStatuses[x - 1] = pos << 5 | 16;
                            this.mNewItemStatuses[pos] = x - 1 << 5 | changeFlag;
                            return true;
                        }
                    }
                }

                curX = snake.x;
                curY = snake.y;
            }

            return false;
        }


        public void dispatchUpdatesTo(@NonNull ListUpdateCallback updateCallback) {
            BatchingListUpdateCallback batchingCallback;
            if (updateCallback instanceof BatchingListUpdateCallback) {
                batchingCallback = (BatchingListUpdateCallback)updateCallback;
            } else {
                batchingCallback = new BatchingListUpdateCallback(updateCallback);
            }

            List<DiffUtil.PostponedUpdate> postponedUpdates = new ArrayList();
            int posOld = this.mOldListSize;
            int posNew = this.mNewListSize;

            for(int snakeIndex = this.mSnakes.size() - 1; snakeIndex >= 0; --snakeIndex) {
                DiffUtil.Snake snake = (DiffUtil.Snake)this.mSnakes.get(snakeIndex);
                int snakeSize = snake.size;
                int endX = snake.x + snakeSize;
                int endY = snake.y + snakeSize;
                if (endX < posOld) {
                    this.dispatchRemovals(postponedUpdates, batchingCallback, endX, posOld - endX, endX);
                }

                if (endY < posNew) {
                    this.dispatchAdditions(postponedUpdates, batchingCallback, endX, posNew - endY, endY);
                }

                for(int i = snakeSize - 1; i >= 0; --i) {
                    if ((this.mOldItemStatuses[snake.x + i] & 31) == 2) {
                        batchingCallback.onChanged(snake.x + i, 1, this.mCallback.getChangePayload(snake.x + i, snake.y + i));
                    }
                }

                posOld = snake.x;
                posNew = snake.y;
            }

            batchingCallback.dispatchLastEvent();
        }

        private static DiffUtil.PostponedUpdate removePostponedUpdate(List<DiffUtil.PostponedUpdate> updates, int pos, boolean removal) {
            for(int i = updates.size() - 1; i >= 0; --i) {
                DiffUtil.PostponedUpdate update = (DiffUtil.PostponedUpdate)updates.get(i);
                if (update.posInOwnerList == pos && update.removal == removal) {
                    updates.remove(i);

                    for(int j = i; j < updates.size(); ++j) {
                        DiffUtil.PostponedUpdate var10000 = (DiffUtil.PostponedUpdate)updates.get(j);
                        var10000.currentPos += removal ? 1 : -1;
                    }

                    return update;
                }
            }

            return null;
        }

        private void dispatchAdditions(List<DiffUtil.PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (!this.mDetectMoves) {
                updateCallback.onInserted(start, count);
            } else {
                label34:
                for(int i = count - 1; i >= 0; --i) {
                    int status = this.mNewItemStatuses[globalIndex + i] & 31;
                    DiffUtil.PostponedUpdate update;
                    switch(status) {
                        case 0:
                            updateCallback.onInserted(start, 1);
                            Iterator var10 = postponedUpdates.iterator();

                            while(true) {
                                if (!var10.hasNext()) {
                                    continue label34;
                                }

                                update = (DiffUtil.PostponedUpdate)var10.next();
                                ++update.currentPos;
                            }
                        case 4:
                        case 8:
                            int pos = this.mNewItemStatuses[globalIndex + i] >> 5;
                            update = removePostponedUpdate(postponedUpdates, pos, true);
                            updateCallback.onMoved(update.currentPos, start);
                            if (status == 4) {
                                updateCallback.onChanged(start, 1, this.mCallback.getChangePayload(pos, globalIndex + i));
                            }
                            break;
                        case 16:
                            postponedUpdates.add(new DiffUtil.PostponedUpdate(globalIndex + i, start, false));
                            break;
                        default:
                            throw new IllegalStateException("unknown flag for pos " + (globalIndex + i) + " " + Long.toBinaryString((long)status));
                    }
                }

            }
        }

        private void dispatchRemovals(List<DiffUtil.PostponedUpdate> postponedUpdates, ListUpdateCallback updateCallback, int start, int count, int globalIndex) {
            if (!this.mDetectMoves) {
                updateCallback.onRemoved(start, count);
            } else {
                label34:
                for(int i = count - 1; i >= 0; --i) {
                    int status = this.mOldItemStatuses[globalIndex + i] & 31;
                    DiffUtil.PostponedUpdate update;
                    switch(status) {
                        case 0:
                            updateCallback.onRemoved(start + i, 1);
                            Iterator var10 = postponedUpdates.iterator();

                            while(true) {
                                if (!var10.hasNext()) {
                                    continue label34;
                                }

                                update = (DiffUtil.PostponedUpdate)var10.next();
                                --update.currentPos;
                            }
                        case 4:
                        case 8:
                            int pos = this.mOldItemStatuses[globalIndex + i] >> 5;
                            update = removePostponedUpdate(postponedUpdates, pos, false);
                            updateCallback.onMoved(start + i, update.currentPos - 1);
                            if (status == 4) {
                                updateCallback.onChanged(update.currentPos - 1, 1, this.mCallback.getChangePayload(globalIndex + i, pos));
                            }
                            break;
                        case 16:
                            postponedUpdates.add(new DiffUtil.PostponedUpdate(globalIndex + i, start + i, true));
                            break;
                        default:
                            throw new IllegalStateException("unknown flag for pos " + (globalIndex + i) + " " + Long.toBinaryString((long)status));
                    }
                }

            }
        }

        @VisibleForTesting
        List<DiffUtil.Snake> getSnakes() {
            return this.mSnakes;
        }
    }

    static class Range {
        int oldListStart;
        int oldListEnd;
        int newListStart;
        int newListEnd;

        public Range() {
        }

        public Range(int oldListStart, int oldListEnd, int newListStart, int newListEnd) {
            this.oldListStart = oldListStart;
            this.oldListEnd = oldListEnd;
            this.newListStart = newListStart;
            this.newListEnd = newListEnd;
        }
    }

    static class Snake {
        int x;
        int y;
        int size;
        boolean removal;
        boolean reverse;

        Snake() {
        }
    }

    public abstract static class ItemCallback<T> {
        public ItemCallback() {
        }

        public abstract boolean areItemsTheSame(@NonNull T var1, @NonNull T var2);

        public abstract boolean areContentsTheSame(@NonNull T var1, @NonNull T var2);

        @Nullable
        public Object getChangePayload(@NonNull T oldItem, @NonNull T newItem) {
            return null;
        }
    }

    public abstract static class Callback {
        public Callback() {
        }

        public abstract int getOldListSize();

        public abstract int getNewListSize();

        public abstract boolean areItemsTheSame(int var1, int var2);

        public abstract boolean areContentsTheSame(int var1, int var2);

        @Nullable
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return null;
        }
    }
}
