// based on NanoKontrol2 by David Granstrom
// https://github.com/davidgranstrom/NanoKontrol2/blob/master/Classes/NanoKontrol2.sc
MidiFighter {
	var <>bank;
	var <>row;
	var <>col;
	var <>knobs;
	var <>buttons;
	classvar <>presets;
	var <>ctls;
	var <>midiOut;

	*new {
		// this.port = this.findPort ? this.port;
		^super.new.init;
	}

	init {
		this.ctls = ();
		this.bank = 0;
		this.row = 0;
		this.col = 0;
		this.knobs = List[];
		this.buttons = List[];

		MIDIClient.init;
		MIDIIn.connectAll;
		this.midiOut = MIDIOut.newByName("Midi Fighter Twister", "Midi Fighter Twister MIDI 1");

		// this.midiOut.control(0,0,127);
		// this.midiOut.control(0,4,0);

		this.assignCtls;

		// this.list;
	}

	assignCtls {
		// knobs and buttons
		128.do{|i|
			var index = (i+1);
			var kn = MFKnob((\kn_++index).asSymbol, i);
			this.knobs.add(kn);
			// convenience method for accessing individual faders
			this.ctls.put(kn.name, kn);

			kn.midiOut = this.midiOut;
		};

		// side buttons
		32.do{|i|
			var index = (i+1);
			var sbtn = MFBtn((\btn_++index).asSymbol, i);
			sbtn.onChange_({|val, cc| if(cc < 4) {this.bank = cc}});
			this.buttons.add(sbtn);
			this.ctls.put(sbtn.name, sbtn);
		}

	}

	// list preset names
	list {
		this.ctls.keysValuesDo{ |k,v| v.debug(k)};
	}

	postln {
		this.knobs.size.debug("knobs");
		this.knobs.do{ |kn| kn.postln };
	}

	rowcol2index {|bank, row, col|
		^(((bank - 1) * 4.pow(2)) + ((row - 1) * 4.pow(1)) + ((col - 1) * 4.pow(0)) + 1).asInteger;
	}

	index2rowcol {|i|
		var bank, row, col;
		var index = i - 1;
		bank = (index/16).asInteger + 1;
		row = (index / 4).asInteger.mod(4) + 1;
		col = index.mod(4) + 1;
		^(bank: bank, row: row, col: col);
	}

	doesNotUnderstand {|selector ... args|
		^this.ctls[selector] ?? { ^super.doesNotUnderstand(selector, args) }
	}

	setBank { |b|
		this.bank = b;
		this.midiOut.control(3, b, 127);
	}

	knob { |b, r, c|
		var index = this.rowcol2index(b,r,c);
		^this.ctls[(\kn_++index).asSymbol];
	}
}

MFController {
	var <key, <chan, <cc, <valA, <valB, >midiOut;
	var state;

	*new {|key, chan, cc, val=0|
		^super.newCopyArgs(("mfc_" ++ key).asSymbol, chan, cc, val, val).init(key, chan, cc);
	}

	init { |key, chan, cc|
		var sym = (key++"_default").asSymbol;
		[chan, cc].debug(sym);
		MIDIdef.cc(sym, {|... args| args.postln }, cc, chan);
	}

	postln {
		"% : % : [%,%] : % : %".format(key, cc, valA, valB, state).postln;
	}

	onChange_ { |func|
		MIDIdef.cc(key, func, cc, chan);
	}

	name {
		^key;
	}

	set { |value|
		midiOut !? {
			midiOut.control(chan,cc,value);
		};
	}

}

MFKnob : MFController {
	var pressed;

	*new{|key, cc, val=0|
		^super.newCopyArgs(key, 0, cc, val, val).init(key, cc);
	}

	init { |akey, acc|
		var knob = (key++"_default").asSymbol;
		var btn = (key++"_default_btn").asSymbol;
 		MIDIdef.cc(knob, {|val|
			if (pressed.booleanValue) {
				valB = val;
			}{
				valA = val;
			};
		}, acc, 0);

		MIDIdef.cc(btn, {|val|
			pressed = val.booleanValue;
			if (pressed) {this.set(valB)} {this.set(valA)};
		}, acc, 1);
	}

	onChangeBtn_ { |func|
		MIDIdef.cc(key++"_btn", func, cc, 1);
	}

}

MFBtn : MFController {
	*new{|key, cc, val=0|
		^super.newCopyArgs(key, 3, cc, val);
	}

	// init { |key, chan, cc|
	// 	var sym = (key++"_default").asSymbol;
	// 	// [chan, cc].debug(sym);
	// 	// MIDIdef.cc(sym, {|... args| args.postln }, cc, chan);
	// }
}