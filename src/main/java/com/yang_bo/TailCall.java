package com.yang_bo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.function.Supplier;

import net.jodah.typetools.TypeResolver;

final class TailCall {

	@FunctionalInterface
	private static interface Result<T> extends Supplier<T> {
	}

	private static class TailCallHandler implements InvocationHandler {
		private Supplier<?> supplier;

		protected Object evaluate() {
			try {
				return this.supplier.get();
			} finally {
				this.supplier = () -> {
					throw new IllegalStateException(
							"The tail call object returned from `TailCall.xxx()` must be directly returned (did you assign a tail call object to a variable by mistake?).");
				};
			}
		}

		public final Result<?> getResult() {
			if (this.supplier instanceof Result<?>) {
				return (Result<?>) this.supplier;
			} else {
				Result<?> result = run(this);
				this.supplier = result;
				return result;
			}
		}

		private TailCallHandler(Supplier<?> supplier) {
			this.supplier = supplier;
		}

		@Override
		public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return method.invoke(getResult().get(), args);
		}

	}

	private static final class ParasiticTailCallHandler extends TailCallHandler {

		public ParasiticTailCallHandler(Supplier<?> supplier) {
			super(supplier);
		}

		@Override
		protected Object evaluate() {
			try {
				return super.evaluate();
			} finally {
				final ArrayDeque<ParasiticTailCallHandler> existingDeque = TAIL_CALL_DEQUE.get();
				if (existingDeque != null) {
					ParasiticTailCallHandler last = existingDeque.removeLast();
					assert this == last;
				}
			}
		}

	}

	private static Result<?> run(TailCallHandler handler) {
		for (;;) {
			final Object finalResultOrProxy;
			try {
				finalResultOrProxy = handler.evaluate();
			} catch (Exception e) {
				return () -> {
					throw e;
				};
			}
			if (Proxy.isProxyClass(finalResultOrProxy.getClass())) {
				final InvocationHandler nextHandler = Proxy.getInvocationHandler(finalResultOrProxy);
				if (nextHandler instanceof TailCallHandler) {
					handler = (TailCallHandler) nextHandler;
				} else {
					return () -> finalResultOrProxy;
				}
			} else {
				return () -> finalResultOrProxy;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T lazy(Supplier<T> supplier) {
		final Class<?> returnType = TypeResolver.resolveRawArguments(Supplier.class, supplier.getClass())[0];
		return (T) Proxy.newProxyInstance(returnType.getClassLoader(), new Class[] { returnType },
				new TailCallHandler(supplier));
	}

	private static final ThreadLocal<ArrayDeque<ParasiticTailCallHandler>> TAIL_CALL_DEQUE = new ThreadLocal<>();

	@SuppressWarnings("unchecked")
	public static <T> T parasitic(Supplier<T> supplier) {
		final ArrayDeque<ParasiticTailCallHandler> existingDeque = TAIL_CALL_DEQUE.get();
		if (existingDeque == null) {
			final ArrayDeque<ParasiticTailCallHandler> deque = new ArrayDeque<>();
			TAIL_CALL_DEQUE.set(deque);
			final Result<?> result = new TailCallHandler(supplier).getResult();
			while (!deque.isEmpty()) {
				deque.removeFirst().getResult();
			}
			return (T) result.get();
		} else {
			final Class<?> returnType = TypeResolver.resolveRawArguments(Supplier.class, supplier.getClass())[0];
			ParasiticTailCallHandler parasiticTailCallHandler = new ParasiticTailCallHandler(supplier);
			T proxy = (T) Proxy.newProxyInstance(returnType.getClassLoader(), new Class[] { returnType },
					parasiticTailCallHandler);
			existingDeque.addLast(parasiticTailCallHandler);
			return proxy;
		}
	}
}
