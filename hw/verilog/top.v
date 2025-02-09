module top(
	input wire 			t_clk,
	input wire 			t_rst,

	inout wire [7:0]	t_uart_data,
	input wire 			t_uart_txe,
	input wire 			t_uart_rxf,
	output wire 		t_uart_wr,
	output wire 		t_uart_rd,

	output wire 		t_gpio_strip_dout,
	output wire 		t_gpio_matrix_dout,

	input wire 			t_gpio_enc_key,
	input wire 			t_gpio_enc_b,
	input wire 			t_gpio_enc_a,

	output wire [3:0]	t_gpio_til311_data,
	output wire 		t_gpio_til311_strobe,
	output wire 		t_gpio_til311_blank
);

	wire clk = t_clk;


	wire srst;
	synchronizer rst_sync(
		.clk(clk),
		.in(t_rst),
		.out(srst)
	);

	reg [7:0] rst_tmr = 8'h0;
	wire por = rst_tmr != 255;
	always @(posedge clk) begin
		if(por) begin
			rst_tmr <= rst_tmr + 1;
		end
	end

	wire rst = ~srst | por;


	wire stxe;
	synchronizer txe_sync(
		.clk(clk),
		.in(t_uart_txe),
		.out(stxe)
	);

	wire srxf;
	synchronizer rxf_sync(
		.clk(clk),
		.in(t_uart_rxf),
		.out(srxf)
	);


	wire [7:0] uart_do;
	wire [7:0] uart_di;
	wire uart_wr;
	wire uart_rd;
	wire uart_oe;

    assign t_uart_data 	= uart_oe ? uart_do : 8'bz;
	assign uart_di 		= t_uart_data;
	assign t_uart_wr 	= ~uart_wr;
	assign t_uart_rd 	= ~uart_rd;


    FedUp soc(
        .clk(clk),
        .reset(rst),

        .io_uart_rdata(uart_di),
		.io_uart_wdata(uart_do),
		.io_uart_txe(stxe),
		.io_uart_rxf(srxf),
		.io_uart_wr(uart_wr),
		.io_uart_rd(uart_rd),
		.io_uart_oe(uart_oe),

		.io_gpio_strip_dout(t_gpio_strip_dout),
		.io_gpio_matrix_dout(t_gpio_matrix_dout),

		.io_gpio_enc_key(t_gpio_enc_key),
		.io_gpio_enc_b(t_gpio_enc_b),
		.io_gpio_enc_a(t_gpio_enc_a),

		.io_gpio_til311_data(t_gpio_til311_data),
		.io_gpio_til311_strobe(t_gpio_til311_strobe),
		.io_gpio_til311_blank(t_gpio_til311_blank)
   );


endmodule