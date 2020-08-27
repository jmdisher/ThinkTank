
// We will define "Likeness" as the global variable used to access the API.
var Likeness = {};

// Global variables used by Likeness.
Likeness.TEMPLATE_MAP = {};
Likeness.OBSERVABLE_GENERATOR_MAP = {};

// ObservableValue is the lowest-level binding element and it actually contains data.
// This means that it needs to know how to notify UI elements when that data changes.
function ObservableValue() {
	let _listeners = [];
	let _value = null;
	
	let _updateValue = function(value) {
		_listeners.forEach(function(elt, index, array) {
			elt(value);
		});
		_value = value;
	}
	
	this.change = function(event) {
		let value = event.target.value;
		_updateValue(value);
	}
	this.register = function(onUpdate) {
		_listeners.push(onUpdate);
	}
	this.getValue = function() {
		return _value;
	}
	this.setValue = function(data) {
		_updateValue(data);
	}
}

// ObservableMap is simpler than ObservableValue in that it doesn't directly change after produced so it doesn't need to notify anyone.
// The underlying ObservableValue instances inside it do this when they are updated, however.  The map just contains them.
function ObservableMap(mapOfValues) {
	let _map = mapOfValues;
	this.getValue = function(name) {
		return _map.get(name);
	}
}

// ObservableArray is somewhat complicated in that it contains other elements which may change, internally, but its shape can change as elements are added/removed.
// This means that it isn't bound to data elements but DOM structures.
function ObservableArray(subTypeName) {
	let _subTypeName = subTypeName;
	let _listeners = [];
	let _observableElements = [];
	// TODO:  This currently just defines how elements are added to the end, not removed or added in the middle.
	// (this will also change the listener callback structure).
	this.addElement = function(newElement) {
		_observableElements.push(newElement);
		_listeners.forEach(function(elt, index, array) {
			elt(newElement);
		});
	}
	this.register = function(onUpdate) {
		// Populate anything we already had.
		_observableElements.forEach(function(elt, index, array) {
			onUpdate(elt);
		});
		// Add this to listen to future updates.
		_listeners.push(onUpdate);
	}
}

function cloneTemplate(type, container, name, observableValue) {
	Likeness.TEMPLATE_MAP[type](container, name, observableValue);
}

function createStringFieldTemplate(type) {
	let id = "field";
	let labelId = "label";
	let form = document.createElement("div");
	form.classList.add("form-group");
	form.classList.add("row");
	let label = document.createElement("label");
	label.id = labelId;
	label.for = id;
	label.classList.add("col-sm-2");
	label.classList.add("col-form-label");
	let wrap = document.createElement("div");
	wrap.classList.add("col-sm-10");
	let input = document.createElement("input");
	input.id = id;
	input.classList.add("form-control");
	wrap.appendChild(input);
	form.appendChild(label);
	form.appendChild(wrap);
	Likeness.TEMPLATE_MAP[type] = function(container, name, observableValue) {
		// Setup the cloned template DOM elements.
		let clone = form.cloneNode(true);
		let inputElement = clone.querySelector("#" + id);
		let labelElement = clone.querySelector("#" + labelId);
		inputElement.addEventListener("change", observableValue.change);
		labelElement.textContent = name;
		container.appendChild(clone);
		
		// Set the initial value.
		inputElement.value = observableValue.getValue();
		
		// Register the new UI elements with the binding callback.
		observableValue.register(function(value) {
			inputElement.value = value;
		});
	}
	Likeness.OBSERVABLE_GENERATOR_MAP[type] = function() {
		return new ObservableValue();
	}
}

function createStructTemplate(type, nameToTypeMap, actionNameToFunctionMap) {
	let card = document.createElement("div");
	card.classList.add("card");
	let cardHeader = document.createElement("div");
	cardHeader.classList.add("card-header");
	let cardBody = document.createElement("div");
	cardBody.classList.add("card-body");
	card.appendChild(cardHeader);
	card.appendChild(cardBody);
	
	Likeness.TEMPLATE_MAP[type] = function(container, name, observableValue) {
		let clone = card.cloneNode(true);
		let cloneHeader = clone.querySelector(".card-header");
		let cloneBody = clone.querySelector(".card-body");
		cloneHeader.textContent = name;
		for (let [name, type] of nameToTypeMap) {
			Likeness.TEMPLATE_MAP[type](cloneBody, name, observableValue.getValue(name));
		}
		for (let [name, action] of actionNameToFunctionMap) {
			let button = document.createElement("button");
			button.textContent = name;
			button.onclick = function(event) {
				action(observableValue);
			}
			cloneBody.appendChild(button);
		}
		container.appendChild(clone);
	}
	Likeness.OBSERVABLE_GENERATOR_MAP[type] = function() {
		let map = new Map();
		for (let [name, type] of nameToTypeMap) {
			map.set(name, Likeness.OBSERVABLE_GENERATOR_MAP[type]());
		}
		return new ObservableMap(map);
	}
}

function createArrayTemplate(type, subtype) {
	let topLevel = document.createElement("div");
	topLevel.classList.add("card");
	
	Likeness.TEMPLATE_MAP[type] = function(container, name, observableValue) {
		let clone = topLevel.cloneNode(true);
		
		// Create the callback which will be called when a new element is added to the given observableValue.
		let onAddElement = function(observableElement) {
			let subContainer = document.createElement("div");
			subContainer.classList.add("row");
			let inner = document.createElement("div");
			inner.classList.add("col-sm-12");
			subContainer.appendChild(inner);
			clone.appendChild(subContainer);
			Likeness.TEMPLATE_MAP[subtype](inner, "", observableElement);
		}
		observableValue.register(onAddElement);
		
		// Append the top-level clone.
		container.appendChild(clone);
	}
	Likeness.OBSERVABLE_GENERATOR_MAP[type] = function() {
		return new ObservableArray(subtype);
	}
}
