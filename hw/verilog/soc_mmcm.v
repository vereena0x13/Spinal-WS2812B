module soc_mmcm(
    input wire clk_in,
    input wire reset,
    output wire clk_out
);

    wire clk_in_b;
    IBUF clk_in_ibufg(
        .I(clk_in),
        .O(clk_in_b)
    );


    wire clk_fbout;
    wire clk_fbout_b;
    wire mmcm_clk_out;

    wire clkfboutb_unused;
    wire clkout0b_unused;
    wire clkout1_unused;
    wire clkout1b_unused;
    wire clkout2_unused;
    wire clkout2b_unused;
    wire clkout3_unused;
    wire clkout3b_unused;
    wire clkout4_unused;
    wire clkout5_unused;
    wire clkout6_unused;
    wire do_unused;
    wire drdy_unused;
    wire psdone_unused;
    wire locked_int;
    wire clkinstopped_unused;
    wire clkfbstopped_unused;

    MMCME2_ADV #(
        .BANDWIDTH            ("OPTIMIZED"),
        .CLKOUT4_CASCADE      ("FALSE"),
        .COMPENSATION         ("ZHOLD"),
        .STARTUP_WAIT         ("FALSE"),
        .DIVCLK_DIVIDE        (2),
        .CLKFBOUT_MULT_F      (15.625),
        .CLKFBOUT_PHASE       (0.000),
        .CLKFBOUT_USE_FINE_PS ("FALSE"),
        .CLKOUT0_DIVIDE_F     (78.125),
        .CLKOUT0_PHASE        (0.000),
        .CLKOUT0_DUTY_CYCLE   (0.500),
        .CLKOUT0_USE_FINE_PS  ("FALSE"),
        .CLKIN1_PERIOD        (10.000)
    ) mmcm(
        .CLKFBOUT            (clk_fbout),
        .CLKFBOUTB           (clkfboutb_unused),
        .CLKOUT0             (mmcm_clk_out),
        .CLKOUT0B            (clkout0b_unused),
        .CLKOUT1             (clkout1_unused),
        .CLKOUT1B            (clkout1b_unused),
        .CLKOUT2             (clkout2_unused),
        .CLKOUT2B            (clkout2b_unused),
        .CLKOUT3             (clkout3_unused),
        .CLKOUT3B            (clkout3b_unused),
        .CLKOUT4             (clkout4_unused),
        .CLKOUT5             (clkout5_unused),
        .CLKOUT6             (clkout6_unused),
        // Input clock control
        .CLKFBIN             (clk_fbout_b),
        .CLKIN1              (clk_in_b),
        .CLKIN2              (1'b0),
        // Tied to always select the primary input clock
        .CLKINSEL            (1'b1),
        // Ports for dynamic reconfiguration
        .DADDR               (7'h0),
        .DCLK                (1'b0),
        .DEN                 (1'b0),
        .DI                  (16'h0),
        .DO                  (do_unused),
        .DRDY                (drdy_unused),
        .DWE                 (1'b0),
        // Ports for dynamic phase shift
        .PSCLK               (1'b0),
        .PSEN                (1'b0),
        .PSINCDEC            (1'b0),
        .PSDONE              (psdone_unused),
        // Other control and status signals
        .LOCKED              (locked_int),
        .CLKINSTOPPED        (clkinstopped_unused),
        .CLKFBSTOPPED        (clkfbstopped_unused),
        .PWRDWN              (1'b0),
        .RST                 (reset)
    );


    BUFG clk_fbout_bufg(
        .I(clk_fbout),
        .O(clk_fbout_b)
    );


    BUFG mmcm_clk_out_bufg(
        .I(mmcm_clk_out),
        .O(clk_out)
    );
    

endmodule