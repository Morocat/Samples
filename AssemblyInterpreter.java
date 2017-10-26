package com.msi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class AssemblyInterpreter {
	
	private static String[] progs = {
            "\n; My first program\nmov  a, 5\ninc  a\ncall function\nmsg  '(5+1)/2 = ', a    ; output message\nend\n\nfunction:\n    div  a, 2\n    ret\n",
            
            "\nmov   a, 5\nmov   b, a\nmov   c, a\ncall  proc_fact\ncall  print\nend\n\nproc_fact:\n    dec   b\n    mul   c, b\n    cmp   b, 1\n    jne   proc_fact\n    ret\n\nprint:\n    msg   a, '! = ', c ; output text\n    ret\n",
            
            "\nmov   a, 8            ; value\nmov   b, 0            ; next\nmov   c, 0            ; counter\nmov   d, 0            ; first\nmov   e, 1            ; second\ncall  proc_fib\ncall  print\nend\n\nproc_fib:\n    cmp   c, 2\n    jl    func_0\n    mov   b, d\n    add   b, e\n    mov   d, e\n    mov   e, b\n    inc   c\n    cmp   c, a\n    jle   proc_fib\n    ret\n\nfunc_0:\n    mov   b, c\n    inc   c\n    jmp   proc_fib\n\nprint:\n    msg   'Term ', a, ' of Fibonacci series is: ', b        ; output text\n    ret\n",
            
            "\nmov   a, 11           ; value1\nmov   b, 3            ; value2\ncall  mod_func\nmsg   'mod(', a, ', ', b, ') = ', d        ; output\nend\n\n; Mod function\nmod_func:\n    mov   c, a        ; temp1\n    div   c, b\n    mul   c, b\n    mov   d, a        ; temp2\n    sub   d, c\n    ret\n",
            
            "\nmov   a, 81         ; value1\nmov   b, 153        ; value2\ncall  init\ncall  proc_gcd\ncall  print\nend\n\nproc_gcd:\n    cmp   c, d\n    jne   loop\n    ret\n\nloop:\n    cmp   c, d\n    jg    a_bigger\n    jmp   b_bigger\n\na_bigger:\n    sub   c, d\n    jmp   proc_gcd\n\nb_bigger:\n    sub   d, c\n    jmp   proc_gcd\n\ninit:\n    cmp   a, 0\n    jl    a_abs\n    cmp   b, 0\n    jl    b_abs\n    mov   c, a            ; temp1\n    mov   d, b            ; temp2\n    ret\n\na_abs:\n    mul   a, -1\n    jmp   init\n\nb_abs:\n    mul   b, -1\n    jmp   init\n\nprint:\n    msg   'gcd(', a, ', ', b, ') = ', c\n    ret\n",
            
            "\ncall  func1\ncall  print\nend\n\nfunc1:\n    call  func2\n    ret\n\nfunc2:\n    ret\n\nprint:\n    msg 'This program should return null'\n",
            
            "\nmov   a, 2            ; value1\nmov   b, 10           ; value2\nmov   c, a            ; temp1\nmov   d, b            ; temp2\ncall  proc_func\ncall  print\nend\n\nproc_func:\n    cmp   d, 1\n    je    continue\n    mul   c, a\n    dec   d\n    call  proc_func\n\ncontinue:\n    ret\n\nprint:\n    msg a, '^', b, ' = ', c\n    ret\n"};

    private static String[] expected = {"(5+1)/2 = 3", 
                                        "5! = 120",
                                        "Term 8 of Fibonacci series is: 21",
                                        "mod(11, 3) = 2",
                                        "gcd(81, 153) = 9",
                                        null,
                                        "2^10 = 1024"};
    
    public static void main(String[] args) {
    	AssemblyInterpreter.doStuff();
    }
    
	public static void doStuff() {
		//for (String s : progs)
		//	System.out.println(s + "\n=======================================\n");
		for (String s :progs)
			System.out.println(interpret(s));
	}
	
	/*******************
	 * Class Variables *
	 *******************/
	private final static boolean LESS_THAN = false, GRT_THAN = true;
	private final static HashMap<String, Method> cmdMap = new HashMap<>();
	
	static {
		try {
			// Math
			cmdMap.put("mov", AssemblyInterpreter.class.getDeclaredMethod("mov"));
			cmdMap.put("inc", AssemblyInterpreter.class.getDeclaredMethod("inc"));
			cmdMap.put("div", AssemblyInterpreter.class.getDeclaredMethod("div"));
			cmdMap.put("add", AssemblyInterpreter.class.getDeclaredMethod("add"));
			cmdMap.put("dec", AssemblyInterpreter.class.getDeclaredMethod("dec"));
			cmdMap.put("mul", AssemblyInterpreter.class.getDeclaredMethod("mul"));
			cmdMap.put("sub", AssemblyInterpreter.class.getDeclaredMethod("sub"));
			
			// Jump
			cmdMap.put("cmp", AssemblyInterpreter.class.getDeclaredMethod("cmp"));
			cmdMap.put("call", AssemblyInterpreter.class.getDeclaredMethod("call"));
			cmdMap.put("ret", AssemblyInterpreter.class.getDeclaredMethod("ret"));
			cmdMap.put("jmp", AssemblyInterpreter.class.getDeclaredMethod("jmp"));
			cmdMap.put("jne", AssemblyInterpreter.class.getDeclaredMethod("jne"));
			cmdMap.put("je", AssemblyInterpreter.class.getDeclaredMethod("je"));
			cmdMap.put("jg", AssemblyInterpreter.class.getDeclaredMethod("jg"));
			cmdMap.put("jge", AssemblyInterpreter.class.getDeclaredMethod("jge"));
			cmdMap.put("jl", AssemblyInterpreter.class.getDeclaredMethod("jl"));
			cmdMap.put("jle", AssemblyInterpreter.class.getDeclaredMethod("jle"));
			
			// MISC
			cmdMap.put("msg", AssemblyInterpreter.class.getDeclaredMethod("msg"));
			cmdMap.put("end", AssemblyInterpreter.class.getDeclaredMethod("end"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String interpret(final String input) {
        AssemblyInterpreter ai = new AssemblyInterpreter();
        ai.parseCmds(input);
        ai.run();
        
        return ai.out;
    }
	
	/**********************
	 * Instance Variables *
	 **********************/
	private HashMap<String, Integer> labelMap = new HashMap<>(); // label, line #
	
	private Stack<String> stack = new Stack<>(); // for use with call/returns. contains line #s
	private List<Command> cmdList = new ArrayList<>();
	
	private boolean done;
	private String out = "";
	
	// Registers
	private int pc; // program counter (line #)
	private int[] reg = new int[26]; // data
	private boolean rEql; // true if the last comparison was equal
	private boolean rCmp; // false for less than, true for greater than
	
	public void run() {
		while (!done) {
			if (pc >= cmdList.size()) {
				out = null;
				return;
			}
			Command cmd = cmdList.get(pc);
			if (!cmd.isLabel) {
				Method method = cmdMap.get(cmd.cmd);
				for (String s : cmd.params)
					stack.push(s);
				if (cmd.cmd.equals("msg"))
					stack.push(String.valueOf(cmd.params.length));
				try {
					method.invoke(this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			pc++;
		}
	}

	private List<String> parseCmds(String input) {
		List<String> cmds = new ArrayList<>();
		String spl[] = input.split("\n");
		int line = 0;
        
        for (String s : spl)
        	cmds.add(s);
        
        for (int i = 0; i < cmds.size(); i++) {
        	String cmdStr = cmds.get(i);
        	
        	if (cmdStr.contains(";")) {
        		cmds.set(i, cmdStr.substring(0, cmdStr.indexOf(';'))); // remove comments
        		cmdStr = cmds.get(i);
        	}
        	
        	// remove empty lines
        	boolean found = false;
        	for (String c : cmdMap.keySet()) {
        		if (cmdStr.contains(c) || cmdStr.contains(":")) { // may be a label
        			found = true;
        			break;
        		}
        	}
        	
        	if (!found)
        		cmds.remove(i--);
        	else {
        		Command cmd = parseCommand(cmdStr);
    			cmdList.add(cmd);
    			if (cmd.isLabel)
    				labelMap.put(cmd.cmd, line);
    			line++;
        	}
        }
        
        //for (String c : cmds)
        //	System.out.println(c);
        
        return cmds;
	}
	
	private Command parseCommand(String cmd) {
		Command c = new Command();
		byte[] arr = cmd.getBytes();
		int i, j;
		
		for (i = 0; i < arr.length; i++)
			if (arr[i] > ' ')
				break; // start of cmd
		
		for (j = i; j < arr.length; j++) {
			if (arr[j] == ':') {  // found a label
				c.isLabel = true;
				break;
			}
			
			if (arr[j] <= ' ')
				break; // end of cmd
		}
		
		c.cmd = cmd.substring(i, j).toLowerCase();
		if (c.isLabel)
			return c;
		
		for (i = j; i < arr.length; i++)
			if (arr[i] > ' ')
				break; // start of parameters
		
		//String[] spl = cmd.substring(i).split(",");
		String[] spl = splitParams(cmd.substring(i));
		if (spl.length == 1 && spl[0].equals("")) // fix non-parameterized cmds
			c.params = new String[0];
		else {
			for (i = 0; i < spl.length; i++)
				spl[i] = spl[i].trim();
		
			c.params = Arrays.copyOf(spl, spl.length);
		}
		
		return c;
	}
	
	private String[] splitParams(String s) {
		ArrayList<String> spl = new ArrayList<>();
		byte[] arr = s.getBytes();
		boolean inQuotes = false;
		int start = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] <= ' ') {
				continue;
			}
			if (arr[i] == '\'') {
				inQuotes = !inQuotes;
			}
			if (arr[i] == ',' && !inQuotes) {
				spl.add(s.substring(start, i));
				start = i + 1;
			}
		}
		spl.add(s.substring(start));
		return (String[]) spl.toArray(new String[0]);
	}
	
	/*****************
	 * Math Commands *
	 *****************/
	
	public void mov() {
		// params are in reverse order
		String p1 = stack.pop();
		String p0 = stack.pop();
		setRegister(p0, getRegister(p1));
	}
	
	public void add() {
		String p1 = stack.pop();
		String p0 = stack.pop();
		setRegister(p0, getRegister(p0) + getRegister(p1));
	}
	
	public void inc() {
		String p0 = stack.pop();
		setRegister(p0, getRegister(p0) + 1);
	}
	
	public void div() {
		String p1 = stack.pop();
		String p0 = stack.pop();
		int reg = getRegister(p0) / getRegister(p1);
		setRegister(p0, reg);
	}
	
	public void dec() {
		String p0 = stack.pop();
		setRegister(p0, getRegister(p0) - 1);
	}

	public void mul() {
		String p1 = stack.pop();
		String p0 = stack.pop();
		setRegister(p0, getRegister(p0) * getRegister(p1));
	}

	public void sub() {
		String p1 = stack.pop();
		String p0 = stack.pop();
		setRegister(p0, getRegister(p0) - getRegister(p1));
	}
	
	/*****************
	 * Jump Commands *
	 *****************/
	public void cmp() {
		String p1 = stack.pop();
		String p0 = stack.pop();
		int i0 = getRegister(p0);
		int i1 = getRegister(p1);
		rEql = i0 == i1;
		if (i0 < i1)
			rCmp = LESS_THAN;
		else
			rCmp = GRT_THAN;
		
	}

	public void call() {
		String p0 = stack.pop();
		if (!labelMap.keySet().contains(p0))
			throw new IllegalArgumentException("Call label doesn't exist");
		stack.push(String.valueOf(pc));
		pc = labelMap.get(p0);
	}

	public void ret() {
		pc = Integer.parseInt(stack.pop());
	}

	public void jmp() {
		String p0 = stack.pop();
		pc = labelMap.get(p0);
	}

	public void jne() {
		String p0 = stack.pop();
		if (!rEql)
			pc = labelMap.get(p0);
	}
	
	public void je() {
		String p0 = stack.pop();
		if (rEql)
			pc = labelMap.get(p0);
	}
	
	public void jg() {
		String p0 = stack.pop();
		if (!rEql && rCmp == GRT_THAN)
			pc = labelMap.get(p0);
	}
	
	public void jge() {
		String p0 = stack.pop();
		if (rEql || rCmp == GRT_THAN)
			pc = labelMap.get(p0);
	}

	public void jl() {
		String p0 = stack.pop();
		if (!rEql && rCmp == LESS_THAN)
			pc = labelMap.get(p0);
	}

	public void jle() {
		String p0 = stack.pop();
		if (rEql || rCmp == LESS_THAN)
			pc = labelMap.get(p0);
	}
	
	/*****************
	 * MISC Commands *
	 *****************/
	public void msg() {
		int numParams = Integer.parseInt(stack.pop());
		for (int i = 0; i < numParams; i++) {
			String s = stack.pop();
			if (s.startsWith("'")) {
				s = s.replace("'", "");
				//System.out.print(s);
				out = s + out;
			} else {
				//System.out.print(getRegister(s));
				out = getRegister(s) + out;
			}
		}
	}

	public void end() {
		done = true;
	}
	
	/*******************
	 * Private Helpers *
	 *******************/
	
	private void setRegister(String reg, int val) {
		char c = reg.toLowerCase().charAt(0);
		this.reg[c - 'a'] = val;
	}
	
	private int getRegister(String reg) {
		char c = reg.toLowerCase().charAt(0);
		if (c >= 'a' && c <= 'z')
			return this.reg[c - 'a'];
		return Integer.parseInt(reg); // might be a constant instead of a register
	}
	
	private static class Command {
		private String[] params;
		private String cmd;
		private boolean isLabel;
	}

}
