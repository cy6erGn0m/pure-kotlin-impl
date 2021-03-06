package org.jetbrains.kotlin.collections


class ArrayList<E> private constructor(
    private var array: Array<E>,
    private var offset: Int = 0,
    private var length: Int = 0,
    private val backing: ArrayList<E>? = null
) : MutableList<E> {

    constructor(initialCapacity: Int = 10) : this(arrayOfLateInitElements(initialCapacity))

    constructor(c: Collection<E>) : this(c.size) {
        addAll(c)
    }

    override val size : Int
        get() = length
    
    override fun isEmpty(): Boolean = length == 0

    override fun get(index: Int): E {
        checkIndex(index)
        return array[offset + index]
    }

    override fun set(index: Int, element: E): E {
        checkIndex(index)
        val old = array[offset + index]
        array[offset + index] = element
        return old
    }

    override fun contains(element: E): Boolean {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return true
            i++
        }
        return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        val it = elements.iterator()
        while (it.hasNext()) {
            if (!contains(it.next()))return false
        }
        return true
    }

    override fun indexOf(element: E): Int {
        var i = 0
        while (i < length) {
            if (array[offset + i] == element) return i
            i++
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        var i = length - 1
        while (i >= 0) {
            if (array[offset + i] == element) return i
            i--
        }
        return -1
    }

    override fun iterator(): MutableIterator<E> = Itr(this)
    override fun listIterator(): MutableListIterator<E> = Itr(this)

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkInsertIndex(index)
        return Itr(this, index)
    }

    override fun add(element: E): Boolean {
        if (backing != null) {
            backing.addAtInternal(offset + length, element)
            array = backing.array
            length++
        } else {
            ensureExtraCapacity()
            array[offset + length++] = element
        }
        return true
    }

    override fun add(index: Int, element: E) {
        checkInsertIndex(index)
        if (backing != null) {
            backing.addAtInternal(offset + index, element)
            array = backing.array
            length++
        } else {
            addAtInternal(offset + index, element)
        }
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkInsertIndex(index)
        val n = elements.size
        if (backing != null) {
            val result = backing.addAllInternal(elements, offset + index, n)
            array = backing.array
            length += n
            return result
        } else {
            return addAllInternal(elements, offset + index, n)
        }
    }

    override fun addAll(elements: Collection<E>): Boolean = addAll(length, elements)

    override fun clear() {
        if (backing != null)
            backing.removeRangeInternal(offset, length)
        else
            array.resetRange(fromIndex = offset, toIndex = offset + length)
        length = 0
    }

    override fun removeAt(index: Int): E {
        checkIndex(index)
        if (backing != null) {
            val old = backing.removeAtInternal(offset + index)
            length--
            return old
        } else {
            return removeAtInternal(offset + index)
        }
    }

    override fun remove(element: E): Boolean {
        val i = indexOf(element)
        if (i >= 0) removeAt(i)
        return i >= 0
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var changed = false
        val it = elements.iterator()
        while (it.hasNext()) {
            if (remove(it.next())) changed = true
        }
        return changed
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (backing != null) {
            val removed = retainAllInternal(elements)
            backing.removeRangeInternal(offset + length, removed)
            return removed > 0
        } else {
            return retainAllInternal(elements) > 0
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkInsertIndex(fromIndex)
        checkInsertIndex(toIndex, fromIndex)
        return ArrayList(array, offset + fromIndex, toIndex - fromIndex, this)
    }

    fun trimToSize() {
        if (length < array.size)
            array = array.copyOfLateInitElements(length)
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity > array.size)
            array = array.copyOfLateInitElements(capacity.coerceAtLeast(array.size * 3 / 2))
    }

    override fun equals(other: Any?): Boolean {
        return other === this ||
            (other is List<*>) &&
            contentEquals(other)
    }

    override fun hashCode(): Int {
        var result = 1
        var i = 0
        while (i < length) {
            result = result * 31 + (array[offset + i]?.hashCode() ?: 0)
            i++
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder(2 + length * 3)
        sb.append("[")
        var i = 0
        while (i < length) {
            if (i > 0) sb.append(", ")
            sb.append(array[offset + i])
            i++
        }
        sb.append("]")
        return sb.toString()
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int = 1) {
        ensureCapacity(length + n)
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= length) throw IndexOutOfBoundsException()
    }

    private fun checkInsertIndex(index: Int, fromIndex: Int = 0) {
        if (index < fromIndex || index > length) throw IndexOutOfBoundsException()
    }

    private fun contentEquals(other: List<*>): Boolean {
        if (length != other.size) return false
        var i = 0
        while (i < length) {
            if (array[offset + i] != other[i]) return false
            i++
        }
        return true
    }

    private fun insertAtInternal(i: Int, n: Int = 1) {
        ensureExtraCapacity(n)
        array.copyRange(fromIndex = i, toIndex = offset + length, destinationIndex = i + n)
        length += n
    }

    private fun addAtInternal(i: Int, element: E) {
        insertAtInternal(i)
        array[i] = element
    }

    private fun addAllInternal(elements: Collection<E>, i: Int, n: Int): Boolean {
        insertAtInternal(i, n)
        var j = 0
        val it = elements.iterator()
        while (j < n) {
            array[i + j] = it.next()
            j++
        }
        return n > 0
    }

    private fun removeAtInternal(i: Int): E {
        val old = array[i]
        array.copyRange(fromIndex = i + 1, toIndex = offset + length, destinationIndex = i)
        array.resetAt(offset + length - 1)
        length--
        return old
    }

    private fun removeRangeInternal(rangeOffset: Int, rangeLength: Int) {
        array.copyRange(fromIndex = rangeOffset + rangeLength, toIndex = length, destinationIndex = rangeOffset)
        array.resetRange(fromIndex = length - rangeLength, toIndex = length)
        length -= rangeLength
    }

    private fun retainAllInternal(elements: Collection<E>): Int {
        var i = 0
        var j = 0
        while (i < length) {
            if (elements.contains(array[offset + i])) {
                array[offset + j++] = array[offset + i++]
            } else {
                i++
            }
        }
        array.resetRange(fromIndex = offset + j, toIndex = length)
        val removed = length - j
        length = j
        return removed
    }

    private class Itr<E>(
            private val list: ArrayList<E>,
            private var index: Int = 0
    ) : MutableListIterator<E> {
        private var lastIndex: Int = -1

        override fun hasPrevious(): Boolean = index > 0
        override fun hasNext(): Boolean = index < list.length

        override fun previousIndex(): Int = index - 1
        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (index <= 0) throw IndexOutOfBoundsException()
            lastIndex = --index
            return list.array[list.offset + lastIndex]
        }

        override fun next(): E {
            if (index >= list.length) throw IndexOutOfBoundsException()
            lastIndex = index++
            return list.array[list.offset + lastIndex]
        }

        override fun set(element: E) {
            list.checkIndex(lastIndex)
            list.array[list.offset + lastIndex] = element
        }

        override fun add(element: E) {
            list.add(index++, element)
            lastIndex = -1
        }

        override fun remove() {
            list.removeAt(lastIndex)
            index = lastIndex
            lastIndex = -1
        }
    }
}

