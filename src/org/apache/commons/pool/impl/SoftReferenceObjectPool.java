/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool.BaseObjectPool;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * A {@link java.lang.ref.SoftReference SoftReference} based
 * {@link ObjectPool}.
 *
 * @param <T> the type of objects held in this pool
 * 
 * @author Rodney Waldhoff
 * @author Sandy McArthur
 * @version $Revision: 1222710 $ $Date: 2011-12-23 10:58:12 -0500 (Fri, 23 Dec 2011) $
 * @since Pool 1.0
 */
public class SoftReferenceObjectPool<T> extends BaseObjectPool<T> implements ObjectPool<T> {
    /**
     * Create a <code>SoftReferenceObjectPool</code> without a factory.
     * {@link #setFactory(PoolableObjectFactory) setFactory} should be called
     * before any attempts to use the pool are made.
     * Generally speaking you should prefer the {@link #SoftReferenceObjectPool(PoolableObjectFactory)} constructor.
     *
     * @see #SoftReferenceObjectPool(PoolableObjectFactory)
     * @deprecated to be removed in pool 2.0.  Use {@link #SoftReferenceObjectPool(PoolableObjectFactory)}.
     */
    @Deprecated
    public SoftReferenceObjectPool() {
        _pool = new ArrayList<SoftReference<T>>();
        _factory = null;
    }

    /**
     * Create a <code>SoftReferenceObjectPool</code> with the specified factory.
     *
     * @param factory object factory to use.
     */
    public SoftReferenceObjectPool(PoolableObjectFactory<T> factory) {
        _pool = new ArrayList<SoftReference<T>>();
        _factory = factory;
    }

    /**
     * Create a <code>SoftReferenceObjectPool</code> with the specified factory and initial idle object count.
     *
     * @param factory object factory to use.
     * @param initSize initial size to attempt to prefill the pool.
     * @throws Exception when there is a problem prefilling the pool.
     * @throws IllegalArgumentException when <code>factory</code> is <code>null</code>.
     * @deprecated because this is a SoftReference pool, prefilled idle obejects may be garbage collected before they are used.
     *      To be removed in Pool 2.0.
     */
    @Deprecated
    public SoftReferenceObjectPool(PoolableObjectFactory<T> factory, int initSize) throws Exception, IllegalArgumentException {
        if (factory == null) {
            throw new IllegalArgumentException("factory required to prefill the pool.");
        }
        _pool = new ArrayList<SoftReference<T>>(initSize);
        _factory = factory;
        PoolUtils.prefill(this, initSize);
    }

    /**
     * <p>Borrow an object from the pool.  If there are no idle instances available in the pool, the configured
     * factory's {@link PoolableObjectFactory#makeObject()} method is invoked to create a new instance.</p>
     * 
     * <p>All instances are {@link PoolableObjectFactory#activateObject(Object) activated} and
     * {@link PoolableObjectFactory#validateObject(Object) validated} before being returned by this
     * method.  If validation fails or an exception occurs activating or validating an idle instance,
     * the failing instance is {@link PoolableObjectFactory#destroyObject(Object) destroyed} and another
     * instance is retrieved from the pool, validated and activated.  This process continues until either the
     * pool is empty or an instance passes validation.  If the pool is empty on activation or
     * it does not contain any valid instances, the factory's <code>makeObject</code> method is used
     * to create a new instance.  If the created instance either raises an exception on activation or
     * fails validation, <code>NoSuchElementException</code> is thrown. Exceptions thrown by <code>MakeObject</code>
     * are propagated to the caller; but other than <code>ThreadDeath</code> or <code>VirtualMachineError</code>,
     * exceptions generated by activation, validation or destroy methods are swallowed silently.</p>
     * 
     * @throws NoSuchElementException if a valid object cannot be provided
     * @throws IllegalStateException if invoked on a {@link #close() closed} pool
     * @throws Exception if an exception occurs creating a new instance
     * @return a valid, activated object instance
     */
    @Override
    public synchronized T borrowObject() throws Exception {
        assertOpen();
        T obj = null;
        boolean newlyCreated = false;
        while(null == obj) {
            if(_pool.isEmpty()) {
                if(null == _factory) {
                    throw new NoSuchElementException();
                } else {
                    newlyCreated = true;
                    obj = _factory.makeObject();
                }
            } else {
                SoftReference<T> ref = _pool.remove(_pool.size() - 1);
                obj = ref.get();
                ref.clear(); // prevent this ref from being enqueued with refQueue.
            }
            if (null != _factory && null != obj) {
                try {
                    _factory.activateObject(obj);
                    if (!_factory.validateObject(obj)) {
                        throw new Exception("ValidateObject failed");
                    }
                } catch (Throwable t) {
                    PoolUtils.checkRethrow(t);
                    try {
                        _factory.destroyObject(obj);
                    } catch (Throwable t2) {
                        PoolUtils.checkRethrow(t2);
                        // Swallowed
                    } finally {
                        obj = null;
                    }
                    if (newlyCreated) {
                        throw new NoSuchElementException(
                            "Could not create a validated object, cause: " +
                            t.getMessage());
                    }
                }
            }
        }
        _numActive++;
        return obj;
    }

    /**
     * <p>Returns an instance to the pool after successful validation and passivation. The returning instance
     * is destroyed if any of the following are true:<ul>
     *   <li>the pool is closed</li>
     *   <li>{@link PoolableObjectFactory#validateObject(Object) validation} fails</li>
     *   <li>{@link PoolableObjectFactory#passivateObject(Object) passivation} throws an exception</li>
     * </ul>
     *</p>
     * 
     * <p>Exceptions passivating or destroying instances are silently swallowed.  Exceptions validating
     * instances are propagated to the client.</p>
     * 
     * @param obj instance to return to the pool
     */
    @Override
    public synchronized void returnObject(T obj) throws Exception {
        boolean success = !isClosed();
        if (_factory != null) {
            if(!_factory.validateObject(obj)) {
                success = false;
            } else {
                try {
                    _factory.passivateObject(obj);
                } catch(Exception e) {
                    success = false;
                }
            }
        }

        boolean shouldDestroy = !success;
        _numActive--;
        if(success) {
            _pool.add(new SoftReference<T>(obj, refQueue));
        }
        notifyAll(); // _numActive has changed

        if (shouldDestroy && _factory != null) {
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void invalidateObject(T obj) throws Exception {
        _numActive--;
        if (_factory != null) {
            _factory.destroyObject(obj);
        }
        notifyAll(); // _numActive has changed
    }

    /**
     * <p>Create an object, and place it into the pool.
     * addObject() is useful for "pre-loading" a pool with idle objects.</p>
     * 
     * <p>Before being added to the pool, the newly created instance is
     * {@link PoolableObjectFactory#validateObject(Object) validated} and 
     * {@link PoolableObjectFactory#passivateObject(Object) passivated}.  If validation
     * fails, the new instance is {@link PoolableObjectFactory#destroyObject(Object) destroyed}.
     * Exceptions generated by the factory <code>makeObject</code> or <code>passivate</code> are
     * propagated to the caller. Exceptions destroying instances are silently swallowed.</p>
     * 
     * @throws IllegalStateException if invoked on a {@link #close() closed} pool
     * @throws Exception when the {@link #getFactory() factory} has a problem creating or passivating an object.
     */
    @Override
    public synchronized void addObject() throws Exception {
        assertOpen();
        if (_factory == null) {
            throw new IllegalStateException("Cannot add objects without a factory.");
        }
        T obj = _factory.makeObject();

        boolean success = true;
        if(!_factory.validateObject(obj)) {
            success = false;
        } else {
            _factory.passivateObject(obj);
        }

        boolean shouldDestroy = !success;
        if(success) {
            _pool.add(new SoftReference<T>(obj, refQueue));
            notifyAll(); // _numActive has changed
        }

        if(shouldDestroy) {
            try {
                _factory.destroyObject(obj);
            } catch(Exception e) {
                // ignored
            }
        }
    }

    /**
     * Returns an approximation not less than the of the number of idle instances in the pool.
     * 
     * @return estimated number of idle instances in the pool
     */
    @Override
    public synchronized int getNumIdle() {
        pruneClearedReferences();
        return _pool.size();
    }

    /**
     * Return the number of instances currently borrowed from this pool.
     *
     * @return the number of instances currently borrowed from this pool
     */
    @Override
    public synchronized int getNumActive() {
        return _numActive;
    }

    /**
     * Clears any objects sitting idle in the pool.
     */
    @Override
    public synchronized void clear() {
        if(null != _factory) {
            Iterator<SoftReference<T>> iter = _pool.iterator();
            while(iter.hasNext()) {
                try {
                    T obj = iter.next().get();
                    if(null != obj) {
                        _factory.destroyObject(obj);
                    }
                } catch(Exception e) {
                    // ignore error, keep destroying the rest
                }
            }
        }
        _pool.clear();
        pruneClearedReferences();
    }

    /**
     * <p>Close this pool, and free any resources associated with it. Invokes
     * {@link #clear()} to destroy and remove instances in the pool.</p>
     * 
     * <p>Calling {@link #addObject} or {@link #borrowObject} after invoking
     * this method on a pool will cause them to throw an
     * {@link IllegalStateException}.</p>
     *
     * @throws Exception never - exceptions clearing the pool are swallowed
     */
    @Override
    public void close() throws Exception {
        super.close();
        clear();
    }

    /**
     * Sets the {@link PoolableObjectFactory factory} this pool uses
     * to create new instances. Trying to change
     * the <code>factory</code> while there are borrowed objects will
     * throw an {@link IllegalStateException}.
     *
     * @param factory the {@link PoolableObjectFactory} used to create new instances.
     * @throws IllegalStateException when the factory cannot be set at this time
     * @deprecated to be removed in pool 2.0
     */
    @Deprecated
    @Override
    public synchronized void setFactory(PoolableObjectFactory<T> factory) throws IllegalStateException {
        assertOpen();
        if(0 < getNumActive()) {
            throw new IllegalStateException("Objects are already active");
        } else {
            clear();
            _factory = factory;
        }
    }

    /**
     * If any idle objects were garbage collected, remove their
     * {@link Reference} wrappers from the idle object pool.
     */
    private void pruneClearedReferences() {
        Reference<? extends T> ref;
        while ((ref = refQueue.poll()) != null) {
            try {
                _pool.remove(ref);
            } catch (UnsupportedOperationException uoe) {
                // ignored
            }
        }
    }
    
    /**
     * Returns the {@link PoolableObjectFactory} used by this pool to create and manage object instances.
     * 
     * @return the factory
     * @since 1.5.5
     */
    public synchronized PoolableObjectFactory<T> getFactory() { 
        return _factory;
    }

    /** My pool. */
    private final List<SoftReference<T>> _pool;

    /** My {@link PoolableObjectFactory}. */
    private PoolableObjectFactory<T> _factory = null;

    /**
     * Queue of broken references that might be able to be removed from <code>_pool</code>.
     * This is used to help {@link #getNumIdle()} be more accurate with minimial
     * performance overhead.
     */
    private final ReferenceQueue<T> refQueue = new ReferenceQueue<T>();

    /** Number of active objects. */
    private int _numActive = 0; //@GuardeBy("this")
}
