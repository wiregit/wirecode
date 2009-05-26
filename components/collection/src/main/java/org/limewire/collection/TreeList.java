package org.limewire.collection;

/*
 *  Copyright 2004-2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A <code>List</code> implementation that is optimized for fast insertions and
 * removals at any index in the list.
 * <p>
 * This list implementation utilizes a tree structure internally to ensure that
 * all insertions and removals are O(log n). This provides much faster performance
 * than both an <code>ArrayList</code> and a <code>LinkedList</code> where elements
 * are inserted and removed repeatedly from anywhere in the list.
 * <p>
 * The following relative performance statistics are indicative of this class:
 * <pre>
 *              get  add  insert  iterate  remove
 * TreeList       3    5       1       2       1
 * ArrayList      1    1      40       1      40
 * LinkedList  5800    1     350       2     325
 * </pre>
 * <code>ArrayList</code> is a good general purpose list implementation.
 * It is faster than <code>TreeList</code> for most operations except inserting
 * and removing in the middle of the list. <code>ArrayList</code> also uses less
 * memory as <code>TreeList</code> uses one object per entry.
 * <p>
 * <code>LinkedList</code> is rarely a good choice of implementation.
 * <code>TreeList</code> is almost always a good replacement for it, although it
 * does use slightly more memory.
 * 
 * @author Joerg Schmuecker
 * @author Stephen Colebourne
 * @since Commons Collections 3.1
 *
 */
public class TreeList<E> extends AbstractList<E> {
//    add; toArray; iterator; insert; get; indexOf; remove
//    TreeList = 1260;7360;3080;  160;   170;3400;  170;
//   ArrayList =  220;1480;1760; 6870;    50;1540; 7200;
//  LinkedList =  270;7360;3350;55860;290720;2910;55200;

    /** The root node in the AVL tree */
    private AVLNode<E> root;

    /** The current size of the list */
    private int size;

    //-----------------------------------------------------------------------
    /**
     * Constructs a new empty list.
     */
    public TreeList() {
        super();
    }

    /**
     * Constructs a new empty list that copies the specified list.
     * 
     * @param coll  the collection to copy
     * @throws NullPointerException if the collection is null
     */
    public TreeList(Collection<? extends E> coll) {
        super();
        addAll(coll);
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the element at the specified index.
     * 
     * @param index  the index to retrieve
     * @return the element at the specified index
     */
    @Override
    public E get(int index) {
        checkInterval(index, 0, size() - 1);
        return root.get(index).getValue();
    }

    /**
     * Gets the current size of the list.
     * 
     * @return the current size
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Gets an iterator over the list.
     * 
     * @return an iterator over the list
     */
    @Override
    public Iterator<E> iterator() {
        // override to go 75% faster
        return listIterator(0);
    }

    /**
     * Gets a ListIterator over the list.
     * 
     * @return the new iterator
     */
    @Override
    public ListIterator<E> listIterator() {
        // override to go 75% faster
        return listIterator(0);
    }

    /**
     * Gets a ListIterator over the list.
     * 
     * @param fromIndex  the index to start from
     * @return the new iterator
     */
    @Override
    public ListIterator<E> listIterator(int fromIndex) {
        // override to go 75% faster
        // cannot use EmptyIterator as iterator.add() must work
        checkInterval(fromIndex, 0, size());
        return new TreeListIterator<E>(this, fromIndex);
    }

    /**
     * Searches for the index of an object in the list.
     * 
     * @return the index of the object, -1 if not found
     */
    @Override
    public int indexOf(Object object) {
        // override to go 75% faster
        if (root == null) {
            return -1;
        }
        return root.indexOf(object, root.relativePosition);
    }

    /**
     * Searches for the presence of an object in the list.
     * 
     * @return true if the object is found
     */
    @Override
    public boolean contains(Object object) {
        return (indexOf(object) >= 0);
    }

    /**
     * Converts the list into an array.
     * 
     * @return the list as an array
     */
    @Override
    public Object[] toArray() {
        // override to go 20% faster
        Object[] array = new Object[size()];
        if (root != null) {
            root.toArray(array, root.relativePosition);
        }
        return array;
    }

    //-----------------------------------------------------------------------
    /**
     * Adds a new element to the list.
     * 
     * @param index  the index to add before
     * @param obj  the element to add
     */
    @Override
    public void add(int index, E obj) {
        modCount++;
        checkInterval(index, 0, size());
        if (root == null) {
            root = new AVLNode<E>(index, obj, null, null);
        } else {
            root = root.insert(index, obj);
        }
        size++;
    }

    /**
     * Sets the element at the specified index.
     * 
     * @param index  the index to set
     * @param obj  the object to store at the specified index
     * @return the previous object at that index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    @Override
    public E set(int index, E obj) {
        checkInterval(index, 0, size() - 1);
        AVLNode<E> node = root.get(index);
        E result = node.value;
        node.setValue(obj);
        return result;
    }
    
    /** Removes the object. */
    @Override
    public boolean remove(Object o) {
        int idx = indexOf(o);
        if(idx != -1)
            return remove(idx) != null;
        else
            return false;
    }

    /**
     * Removes the element at the specified index.
     * 
     * @param index  the index to remove
     * @return the previous object at that index
     */
    @Override
    public E remove(int index) {
        modCount++;
        checkInterval(index, 0, size() - 1);
        E result = get(index);
        root = root.remove(index);
        size--;
        return result;
    }

    /**
     * Clears the list, removing all entries.
     */
    @Override
    public void clear() {
        modCount++;
        root = null;
        size = 0;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks whether the index is valid.
     * 
     * @param index  the index to check
     * @param startIndex  the first allowed index
     * @param endIndex  the last allowed index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    private void checkInterval(int index, int startIndex, int endIndex) {
        if (index < startIndex || index > endIndex) {
            throw new IndexOutOfBoundsException("Invalid index:" + index + ", size=" + size());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Implements an AVLNode which keeps the offset updated.
     * <p>
     * This node contains the real work.
     * TreeList is just there to implement {@link java.util.List}.
     * The nodes don't know the index of the object they are holding.  They
     * do know however their position relative to their parent node.
     * This allows to calculate the index of a node while traversing the tree.
     * <p>
     * The Faedelung calculation stores a flag for both the left and right child
     * to indicate if they are a child (false) or a link as in linked list (true).
     */
    static class AVLNode<E> {
        /** The left child node or the predecessor if {@link #leftIsPrevious}.*/
        private AVLNode<E> left;
        /** Flag indicating that left reference is not a subtree but the predecessor. */
        private boolean leftIsPrevious;
        /** The right child node or the successor if {@link #rightIsNext}. */
        private AVLNode<E> right;
        /** Flag indicating that right reference is not a subtree but the successor. */
        private boolean rightIsNext;
        /** How many levels of left/right are below this one. */
        private int height;
        /** The relative position, root holds absolute position. */
        private int relativePosition;
        /** The stored element. */
        private E value;

        /**
         * Constructs a new node with a relative position.
         * 
         * @param relativePosition  the relative position of the node
         * @param obj  the value for the ndoe
         * @param rightFollower the node with the value following this one
         * @param leftFollower the node with the value leading this one
         */
        private AVLNode(int relativePosition, E obj, AVLNode<E> rightFollower, AVLNode<E> leftFollower) {
            this.relativePosition = relativePosition;
            value = obj;
            rightIsNext = true;
            leftIsPrevious = true;
            right = rightFollower;
            left = leftFollower;
        }

        /**
         * Gets the value.
         * 
         * @return the value of this node
         */
        E getValue() {
            return value;
        }

        /**
         * Sets the value.
         * 
         * @param obj  the value to store
         */
        void setValue(E obj) {
            this.value = obj;
        }

        /**
         * Locate the element with the given index relative to the
         * offset of the parent of this node.
         */
        AVLNode<E> get(int index) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe == 0) {
                return this;
            }

            AVLNode<E> nextNode = ((indexRelativeToMe < 0) ? getLeftSubTree() : getRightSubTree());
            if (nextNode == null) {
                return null;
            }
            return nextNode.get(indexRelativeToMe);
        }

        /**
         * Locate the index that contains the specified object.
         */
        int indexOf(Object object, int index) {
            if (getLeftSubTree() != null) {
                int result = left.indexOf(object, index + left.relativePosition);
                if (result != -1) {
                    return result;
                }
            }
            if (value == null ? value == object : value.equals(object)) {
                return index;
            }
            if (getRightSubTree() != null) {
                return right.indexOf(object, index + right.relativePosition);
            }
            return -1;
        }

        /**
         * Stores the node and its children into the array specified.
         * 
         * @param array the array to be filled
         * @param index the index of this node
         */
        void toArray(Object[] array, int index) {
            array[index] = value;
            if (getLeftSubTree() != null) {
                left.toArray(array, index + left.relativePosition);
            }
            if (getRightSubTree() != null) {
                right.toArray(array, index + right.relativePosition);
            }
        }

        /**
         * Gets the next node in the list after this one.
         * 
         * @return the next node
         */
        AVLNode<E> next() {
            if (rightIsNext || right == null) {
                return right;
            }
            return right.min();
        }

        /**
         * Gets the node in the list before this one.
         * 
         * @return the previous node
         */
        AVLNode<E> previous() {
            if (leftIsPrevious || left == null) {
                return left;
            }
            return left.max();
        }

        /**
         * Inserts a node at the position index.  
         * 
         * @param index is the index of the position relative to the position of 
         * the parent node.
         * @param obj is the object to be stored in the position.
         */
        AVLNode<E> insert(int index, E obj) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe <= 0) {
                return insertOnLeft(indexRelativeToMe, obj);
            } else {
                return insertOnRight(indexRelativeToMe, obj);
            }
        }

        private AVLNode<E> insertOnLeft(int indexRelativeToMe, E obj) {
            AVLNode<E> ret = this;

            if (getLeftSubTree() == null) {
                setLeft(new AVLNode<E>(-1, obj, this, left), null);
            } else {
                setLeft(left.insert(indexRelativeToMe, obj), null);
            }

            if (relativePosition >= 0) {
                relativePosition++;
            }
            ret = balance();
            recalcHeight();
            return ret;
        }

        private AVLNode<E> insertOnRight(int indexRelativeToMe, E obj) {
            AVLNode<E> ret = this;

            if (getRightSubTree() == null) {
                setRight(new AVLNode<E>(+1, obj, right, this), null);
            } else {
                setRight(right.insert(indexRelativeToMe, obj), null);
            }
            if (relativePosition < 0) {
                relativePosition--;
            }
            ret = balance();
            recalcHeight();
            return ret;
        }

        //-----------------------------------------------------------------------
        /**
         * Gets the left node, returning null if its a faedelung.
         */
        private AVLNode<E> getLeftSubTree() {
            return (leftIsPrevious ? null : left);
        }

        /**
         * Gets the right node, returning null if its a faedelung.
         */
        private AVLNode<E> getRightSubTree() {
            return (rightIsNext ? null : right);
        }

        /**
         * Gets the rightmost child of this node.
         * 
         * @return the rightmost child (greatest index)
         */
        private AVLNode<E> max() {
            return (getRightSubTree() == null) ? this : right.max();
        }

        /**
         * Gets the leftmost child of this node.
         * 
         * @return the leftmost child (smallest index)
         */
        private AVLNode<E> min() {
            return (getLeftSubTree() == null) ? this : left.min();
        }

        /**
         * Removes the node at a given position.
         * 
         * @param index is the index of the element to be removed relative to the position of 
         * the parent node of the current node.
         */
        AVLNode<E> remove(int index) {
            int indexRelativeToMe = index - relativePosition;

            if (indexRelativeToMe == 0) {
                return removeSelf();
            }
            if (indexRelativeToMe > 0) {
                setRight(right.remove(indexRelativeToMe), right.right);
                if (relativePosition < 0) {
                    relativePosition++;
                }
            } else {
                setLeft(left.remove(indexRelativeToMe), left.left);
                if (relativePosition > 0) {
                    relativePosition--;
                }
            }
            recalcHeight();
            return balance();
        }

        private AVLNode<E> removeMax() {
            if (getRightSubTree() == null) {
                return removeSelf();
            }
            setRight(right.removeMax(), right.right);
            if (relativePosition < 0) {
                relativePosition++;
            }
            recalcHeight();
            return balance();
        }

        private AVLNode<E> removeMin() {
            if (getLeftSubTree() == null) {
                return removeSelf();
            }
            setLeft(left.removeMin(), left.left);
            if (relativePosition > 0) {
                relativePosition--;
            }
            recalcHeight();
            return balance();
        }

        /**
         * Removes this node from the tree.
         *
         * @return the node that replaces this one in the parent
         */
        private AVLNode<E> removeSelf() {
            if (getRightSubTree() == null && getLeftSubTree() == null) {
                return null;
            }
            if (getRightSubTree() == null) {
                if (relativePosition > 0) {
                    left.relativePosition += relativePosition + (relativePosition > 0 ? 0 : 1);
                }
                left.max().setRight(null, right);
                return left;
            }
            if (getLeftSubTree() == null) {
                right.relativePosition += relativePosition - (relativePosition < 0 ? 0 : 1);
                right.min().setLeft(null, left);
                return right;
            }

            if (heightRightMinusLeft() > 0) {
                // more on the right, so delete from the right
                AVLNode<E> rightMin = right.min();
                value = rightMin.value;
                if (leftIsPrevious) {
                    left = rightMin.left;
                }
                right = right.removeMin();
                if (relativePosition < 0) {
                    relativePosition++;
                }
            } else {
                // more on the left or equal, so delete from the left
                AVLNode<E> leftMax = left.max();
                value = leftMax.value;
                if (rightIsNext) {
                    right = leftMax.right;
                }
                AVLNode<E> leftPrevious = left.left;
                left = left.removeMax();
                if (left == null) {
                    // special case where left that was deleted was a double link
                    // only occurs when height difference is equal
                    left = leftPrevious;
                    leftIsPrevious = true;
                }
                if (relativePosition > 0) {
                    relativePosition--;
                }
            }
            recalcHeight();
            return this;
        }

        //-----------------------------------------------------------------------
        /**
         * Balances according to the AVL algorithm.
         */
        private AVLNode<E> balance() {
            switch (heightRightMinusLeft()) {
                case 1 :
                case 0 :
                case -1 :
                    return this;
                case -2 :
                    if (left.heightRightMinusLeft() > 0) {
                        setLeft(left.rotateLeft(), null);
                    }
                    return rotateRight();
                case 2 :
                    if (right.heightRightMinusLeft() < 0) {
                        setRight(right.rotateRight(), null);
                    }
                    return rotateLeft();
                default :
                    throw new RuntimeException("tree inconsistent!");
            }
        }

        /**
         * Gets the relative position.
         */
        private int getOffset(AVLNode node) {
            if (node == null) {
                return 0;
            }
            return node.relativePosition;
        }

        /**
         * Sets the relative position.
         */
        private int setOffset(AVLNode<E> node, int newOffest) {
            if (node == null) {
                return 0;
            }
            int oldOffset = getOffset(node);
            node.relativePosition = newOffest;
            return oldOffset;
        }

        /**
         * Sets the height by calculation.
         */
        private void recalcHeight() {
            height = Math.max(
                getLeftSubTree() == null ? -1 : getLeftSubTree().height,
                getRightSubTree() == null ? -1 : getRightSubTree().height) + 1;
        }

        /**
         * Returns the height of the node or -1 if the node is null.
         */
        private int getHeight(AVLNode<E> node) {
            return (node == null ? -1 : node.height);
        }

        /**
         * Returns the height difference right - left
         */
        private int heightRightMinusLeft() {
            return getHeight(getRightSubTree()) - getHeight(getLeftSubTree());
        }

        private AVLNode<E> rotateLeft() {
            AVLNode<E> newRoot = right; // can't be faedelung!
            AVLNode<E> movedNode = getRightSubTree().getLeftSubTree();

            int newTopPosition = relativePosition + getOffset(newRoot);
            int myNewPosition = -newRoot.relativePosition;
            int movedPosition = getOffset(newRoot) + getOffset(movedNode);

            setRight(movedNode, newRoot);
            newRoot.setLeft(this, null);

            setOffset(newRoot, newTopPosition);
            setOffset(this, myNewPosition);
            setOffset(movedNode, movedPosition);
            return newRoot;
        }

        private AVLNode<E> rotateRight() {
            AVLNode<E> newRoot = left; // can't be faedelung
            AVLNode<E> movedNode = getLeftSubTree().getRightSubTree();

            int newTopPosition = relativePosition + getOffset(newRoot);
            int myNewPosition = -newRoot.relativePosition;
            int movedPosition = getOffset(newRoot) + getOffset(movedNode);

            setLeft(movedNode, newRoot);
            newRoot.setRight(this, null);

            setOffset(newRoot, newTopPosition);
            setOffset(this, myNewPosition);
            setOffset(movedNode, movedPosition);
            return newRoot;
        }

        /**
         * Sets the left field to the node, or the previous node if that is null
         *
         * @param node  the new left subtree node
         * @param previous  the previous node in the linked list
         */
        private void setLeft(AVLNode<E> node, AVLNode<E> previous) {
            leftIsPrevious = (node == null);
            left = (leftIsPrevious ? previous : node);
            recalcHeight();
        }

        /**
         * Sets the right field to the node, or the next node if that is null
         *
         * @param node  the new left subtree node
         * @param next  the next node in the linked list
         */
        private void setRight(AVLNode<E> node, AVLNode<E> next) {
            rightIsNext = (node == null);
            right = (rightIsNext ? next : node);
            recalcHeight();
        }

//      private void checkFaedelung() {
//          AVLNode maxNode = left.max();
//          if (!maxNode.rightIsFaedelung || maxNode.right != this) {
//              throw new RuntimeException(maxNode + " should right-faedel to " + this);
//          }
//          AVLNode minNode = right.min();
//          if (!minNode.leftIsFaedelung || minNode.left != this) {
//              throw new RuntimeException(maxNode + " should left-faedel to " + this);
//          }
//      }
//
//        private int checkTreeDepth() {
//            int hright = (getRightSubTree() == null ? -1 : getRightSubTree().checkTreeDepth());
//            //          System.out.print("checkTreeDepth");
//            //          System.out.print(this);
//            //          System.out.print(" left: ");
//            //          System.out.print(_left);
//            //          System.out.print(" right: ");
//            //          System.out.println(_right);
//
//            int hleft = (left == null ? -1 : left.checkTreeDepth());
//            if (height != Math.max(hright, hleft) + 1) {
//                throw new RuntimeException(
//                    "height should be max" + hleft + "," + hright + " but is " + height);
//            }
//            return height;
//        }
//
//        private int checkLeftSubNode() {
//            if (getLeftSubTree() == null) {
//                return 0;
//            }
//            int count = 1 + left.checkRightSubNode();
//            if (left.relativePosition != -count) {
//                throw new RuntimeException();
//            }
//            return count + left.checkLeftSubNode();
//        }
//        
//        private int checkRightSubNode() {
//            AVLNode right = getRightSubTree();
//            if (right == null) {
//                return 0;
//            }
//            int count = 1;
//            count += right.checkLeftSubNode();
//            if (right.relativePosition != count) {
//                throw new RuntimeException();
//            }
//            return count + right.checkRightSubNode();
//        }

        /**
         * Used for debugging.
         */
        @Override
        public String toString() {
            return "AVLNode(" + relativePosition + "," + (left != null) + "," + value +
                "," + (getRightSubTree() != null) + ", faedelung " + rightIsNext + " )";
        }
    }

    /**
     * A list iterator over the linked list.
     */
    static class TreeListIterator<E> implements ListIterator<E> {
        /** The parent list */
        protected final TreeList<E> parent;
        /**
         * Cache of the next node that will be returned by {@link #next()}.
         */
        protected AVLNode<E> next;
        /**
         * The index of the next node to be returned.
         */
        protected int nextIndex;
        /**
         * Cache of the last node that was returned by {@link #next()}
         * or {@link #previous()}.
         */
        protected AVLNode<E> current;
        /**
         * The index of the last node that was returned.
         */
        protected int currentIndex;
        /**
         * The modification count that the list is expected to have. If the list
         * doesn't have this count, then a
         * {@link java.util.ConcurrentModificationException} may be thrown by
         * the operations.
         */
        protected int expectedModCount;

        /**
         * Create a ListIterator for a list.
         * 
         * @param parent  the parent list
         * @param fromIndex  the index to start at
         */
        protected TreeListIterator(TreeList<E> parent, int fromIndex) throws IndexOutOfBoundsException {
            super();
            this.parent = parent;
            this.expectedModCount = parent.modCount;
            this.next = (parent.root == null ? null : parent.root.get(fromIndex));
            this.nextIndex = fromIndex;
            this.currentIndex = -1;
        }

        /**
         * Checks the modification count of the list is the value that this
         * object expects.
         * 
         * @throws ConcurrentModificationException If the list's modification
         * count isn't the value that was expected.
         */
        protected void checkModCount() {
            if (parent.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            return (nextIndex < parent.size());
        }

        public E next() {
            checkModCount();
            if (!hasNext()) {
                throw new NoSuchElementException("No element at index " + nextIndex + ".");
            }
            if (next == null) {
                next = parent.root.get(nextIndex);
            }
            E value = next.getValue();
            current = next;
            currentIndex = nextIndex++;
            next = next.next();
            return value;
        }

        public boolean hasPrevious() {
            return (nextIndex > 0);
        }

        public E previous() {
            checkModCount();
            if (!hasPrevious()) {
                throw new NoSuchElementException("Already at start of list.");
            }
            if (next == null) {
                next = parent.root.get(nextIndex - 1);
            } else {
                next = next.previous();
            }
            E value = next.getValue();
            current = next;
            currentIndex = --nextIndex;
            return value;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex() - 1;
        }

        public void remove() {
            checkModCount();
            if (currentIndex == -1) {
                throw new IllegalStateException();
            }
            if (nextIndex == currentIndex) {
                // remove() following previous()
                next = next.next();
                parent.remove(currentIndex);
            } else {
                // remove() following next()
                parent.remove(currentIndex);
                nextIndex--;
            }
            current = null;
            currentIndex = -1;
            expectedModCount++;
        }

        public void set(E obj) {
            checkModCount();
            if (current == null) {
                throw new IllegalStateException();
            }
            current.setValue(obj);
        }

        public void add(E obj) {
            checkModCount();
            parent.add(nextIndex, obj);
            current = null;
            currentIndex = -1;
            nextIndex++;
            expectedModCount++;
        }
    }

}
