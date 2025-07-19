package com.coinex.plugin.utils

class ObservableMap<K, V>(
    private val delegate: MutableMap<K, V> = mutableMapOf(),
    private val onAdd: (K, V) -> Unit = { _, _ -> },
    private val onRemove: (K, V) -> Unit = { _, _ -> },
    private val onUpdate: (K, V, V) -> Unit = { _, _, _ -> }
) : MutableMap<K, V> by delegate {

    override fun put(key: K, value: V): V? {
        return delegate[key]?.let { oldValue ->
            delegate.put(key, value).also {
                onUpdate(key, oldValue, value)
            }
        } ?: delegate.put(key, value).also {
            onAdd(key, value)
        }
    }

    override fun remove(key: K): V? {
        return delegate.remove(key)?.also { value ->
            onRemove(key, value)
        }
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) -> put(key, value) }
    }

    override fun clear() {
        delegate.keys.toList().forEach { remove(it) }
    }
}