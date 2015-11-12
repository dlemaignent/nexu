/**
 * © Nowina Solutions, 2015-2015
 *
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu.view.core;

import java.io.IOException;
import java.util.Arrays;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An <code>UIOperation</code> controls the user interaction with the {@link Flow}.
 * The {@link Flow} triggers the <code>UIOperation</code> and calls the method {@link #perform()}.
 * When the user finished the operation, the {@link UIOperationController} notifies the <code>UIOperation</code>
 * through the method {@link #signalEnd(Object)}.
 * 
 * @author david.naramski
 *
 * @param <R> The return type of the operation.
 */
public class UIOperation<R> implements Operation<R> {

	private static final Logger logger = LoggerFactory.getLogger(UIOperation.class.getName());

	private transient Object lock = new Object();
	private transient volatile OperationResult<R> result = null;

	private final UIDisplay display;
	private final String fxml;
	private final Object[] params;
	
	private transient Parent root;
	private transient UIOperationController<R> controller;
	
	public UIOperation(final UIDisplay display, final String fxml, final Object... params) {
		super();
		this.display = display;
		this.fxml = fxml;
		this.params = params;
	}
	
	@Override
	public final OperationResult<R> perform() {		
		logger.info("Loading " + fxml + " view");
		final FXMLLoader loader = new FXMLLoader();
		try {
			loader.load(getClass().getResourceAsStream(fxml));
		} catch(final IOException e) {
			throw new RuntimeException(e);
		}

	    root = loader.getRoot();
		controller = loader.getController();
		controller.init(params);
		controller.setUIOperation(this);

		display.displayAndWaitUIOperation(this);
		return result;
	}
	
	public void waitEnd() throws InterruptedException {
		String name = getOperationName();
		logger.info("Thread " + Thread.currentThread().getName() + " wait on " + name);
		synchronized (lock) {
			lock.wait();
		}
		logger.info("Thread " + Thread.currentThread().getName() + " resumed on " + name);
	}

	/**
	 * When the user has finished performing the operation, the UIOperation must call the "signalEnd()" method to resume the UIFlow.
	 * 
	 * @param result
	 */
	public final void signalEnd(R result) {
		String name = getOperationName();
		logger.info("Notify from " + Thread.currentThread().getName() + " on " + name);
		notifyResult(new OperationResult<>(result));
	}

	private void notifyResult(OperationResult<R> result) {
		this.result = result;
		synchronized (lock) {
			lock.notify();
		}
	}

	public final void signalUserCancel() {
		notifyResult(new OperationResult<>(OperationStatus.USER_CANCEL));
	}

	private String getOperationName() {
		return this.controller.getClass().getSimpleName();
	}

	public Parent getRoot() {
		return root;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((display == null) ? 0 : display.hashCode());
		result = prime * result + ((fxml == null) ? 0 : fxml.hashCode());
		result = prime * result + Arrays.hashCode(params);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UIOperation<?> other = (UIOperation<?>) obj;
		if (display == null) {
			if (other.display != null)
				return false;
		} else if (!display.equals(other.display))
			return false;
		if (fxml == null) {
			if (other.fxml != null)
				return false;
		} else if (!fxml.equals(other.fxml))
			return false;
		if (!Arrays.equals(params, other.params))
			return false;
		return true;
	}

}
