/* Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef _PKT_STATS_H_
#define _PKT_STATS_H_

/* Types of packet log events.
 * Tx stats will be sent from driver with the help of multiple events.
 * Need to parse the events PKTLOG_TYPE_TX_CTRL and PKTLOG_TYPE_TX_STAT
 * as of now for the required stats. Rest of the events can ignored.
 */
#define PKTLOG_TYPE_TX_CTRL         1
#define PKTLOG_TYPE_TX_STAT         2
#define PKTLOG_TYPE_TX_MSDU_ID      3
#define PKTLOG_TYPE_TX_FRM_HDR      4
/* Rx stats will be sent from driver with event ID- PKTLOG_TYPE_RX_STAT */
#define PKTLOG_TYPE_RX_STAT         5
#define PKTLOG_TYPE_RC_FIND         6
#define PKTLOG_TYPE_RC_UPDATE       7
#define PKTLOG_TYPE_TX_VIRT_ADDR    8
#define PKTLOG_TYPE_MAX             9

/* Format of the packet stats event*/
typedef struct {
    u16 flags;
    u16 missed_cnt;
    u16 log_type;
    u16 size;
    u32 timestamp;
} __attribute__((packed)) wh_pktlog_hdr_t;

/*Rx stats specific structures. */

struct rx_mpdu_start {
    u32 reserved1                       : 13; //[12:0]
    u32 encrypted                       :  1; //[13]
    u32 retry                           :  1; //[14]
    u32 reserved2                       :  1; //[15]
    u32 seq_num                         : 12; //[27:16]
    u32 reserved3                       :  4; //[31:28]
    u32 reserved4;
    u32 reserved5                       : 28; //[27:0]
    u32 tid                             :  4; //[31:28]
} __attribute__((packed));

/*Indicates the decap-format of the packet*/
enum {
    RAW=0,      // RAW: No decapsulation
    NATIVEWIFI,
    ETHERNET2,  // (DIX)
    ETHERNET    // (SNAP/LLC)
};

struct rx_msdu_start {
    u32 reserved1[2];
    u32 reserved2                       :  8; //[7:0]
    u32 decap_format                    :  2; //[9:8]
    u32 reserved3                       : 22; //[31:10]
} __attribute__((packed));

struct rx_mpdu_end {
    u32 reserved1                       : 29; //[28:0]
    u32 tkip_mic_err                    :  1; //[29]
    u32 reserved2                       :  2; //[31:30]
} __attribute__((packed));

#define PREAMBLE_L_SIG_RATE     0x04
#define PREAMBLE_VHT_SIG_A_1    0x08
#define PREAMBLE_VHT_SIG_A_2    0x0c

#define BITMASK(x) ((1<<(x)) - 1 )
/* Contains MCS related stats */
struct rx_ppdu_start {
    u32 reserved1[4];
    u32 rssi_comb                       :  8; //[7:0]
    u32 reserved2                       : 24; //[31:8]
    u32 l_sig_rate                      :  4; //[3:0]
    u32 l_sig_rate_select               :  1; //[4]
    u32 reserved3                       : 19; //[23:5]
    u32 preamble_type                   :  8; //[31:24]
    u32 ht_sig_vht_sig_a_1              : 24; //[23:0]
    u32 reserved4                       :  8; //[31:24]
    u32 ht_sig_vht_sig_a_2              : 24; //[23:0]
    u32 reserved5                       :  8; //[31:25]
    u32 reserved6[2];
} __attribute__((packed));

struct rx_ppdu_end {
    u32 reserved1[17];
    u32 wb_timestamp;
    u32 reserved2[4];
} __attribute__((packed));

#define RX_HTT_HDR_STATUS_LEN 64
typedef struct {
    u32 reserved1[2];
    struct rx_mpdu_start mpdu_start;
    struct rx_msdu_start msdu_start;
    u32 reserved2[5];
    struct rx_mpdu_end   mpdu_end;
    struct rx_ppdu_start ppdu_start;
    struct rx_ppdu_end   ppdu_end;
    char rx_hdr_status[RX_HTT_HDR_STATUS_LEN];
}__attribute__((packed)) rb_pkt_stats_t;

/*Tx stats specific structures. */
struct ppdu_status {
    u32 reserved1                       : 31; //[30:0]
    u32 tx_ok                           :  1; //[31]
    u32 reserved2[10];
    u32 ack_rssi_ave                    :  8; //[7:0]
    u32 reserved3                       : 16; //[23:8]
    u32 total_tries                     :  5; //[28:24]
    u32 reserved4                       :  3; //[31:29]
    u32 reserved5[4];
} __attribute__((packed));

/*Contains tx timestamp*/
struct try_status {
    u32 timestamp                       : 23; //[22:0]
    u32 reserved                        :  9; //[23]
} __attribute__((packed));

struct try_list {
    struct try_status try_00;
    u32 reserved[15];
} __attribute__((packed));


struct tx_ppdu_end {
    struct try_list try_list;
    struct ppdu_status stat;
} __attribute__((packed));

/*Tx MCS and data rate ralated stats */
struct series_bw {
    u32 reserved1                       : 28; //[27:0]
    u32 short_gi                        :  1; //[28]
    u32 reserved2                       :  3; //[31:29]
    u32 reserved3                       : 24; //[23:21]
    u32 rate                            :  4; //[27:24]
    u32 nss                             :  2; //[29:28]
    u32 preamble_type                   :  2; //[31:30]
    u32 reserved4[2];
} __attribute__((packed));

enum tx_bw {
    BW_20_MHZ,
    BW_40_MHZ,
    BW_80_MHZ,
    BW_160_MHZ
};

#define DATA_PROTECTED 14
struct tx_ppdu_start {
    u32 reserved1[2];
    u32 start_seq_num                   : 12; //[11:0]
    u32 reserved2                       : 20; //[31:12]
    u32 reserved3[11];
    u32 reserved4                       : 16; //[15:0]
    u32 frame_control                   : 16; //[31:16]
    u32 reserved5                       : 16; //[23:21]
    u32 qos_ctl                         : 16; //[31:16]
    u32 reserved6[4];
    u32 reserved7                       : 24; //[23:21]
    u32 valid_s0_bw20                   :  1; //[24]
    u32 valid_s0_bw40                   :  1; //[25]
    u32 valid_s0_bw80                   :  1; //[26]
    u32 valid_s0_bw160                  :  1; //[27]
    u32 valid_s1_bw20                   :  1; //[28]
    u32 valid_s1_bw40                   :  1; //[29]
    u32 valid_s1_bw80                   :  1; //[30]
    u32 valid_s1_bw160                  :  1; //[31]
    struct series_bw s0_bw20;
    struct series_bw s0_bw40;
    struct series_bw s0_bw80;
    struct series_bw s0_bw160;
    struct series_bw s1_bw20;
    struct series_bw s1_bw40;
    struct series_bw s1_bw80;
    struct series_bw s1_bw160;
    u32 reserved8[3];
} __attribute__((packed));

#define PKTLOG_MAX_TXCTL_WORDS 57 /* +2 words for bitmap */
typedef struct {
    u32 reserved1[3];
    union {
        u32 txdesc_ctl[PKTLOG_MAX_TXCTL_WORDS];
        struct tx_ppdu_start ppdu_start;
    }u;
} __attribute__((packed)) wh_pktlog_txctl;

/* Required stats are spread across multiple
 * events(PKTLOG_TYPE_TX_CTRL and PKTLOG_TYPE_TX_STAT here).
 * Need to aggregate the stats collected in each event and write to the
 * ring buffer only after receiving all the expected stats.
 * Need to preserve the stats in hal_info till then and use tx_stats_events
 * flag to track the events.
 * prev_seq_no: Can used to track the events that come from driver and identify
 * if any event is missed.
 */

#define RING_BUF_ENTRY_SIZE 512
struct pkt_stats_s {
    u8 tx_stats_events;
    u32 prev_seq_no;
    /* TODO: Need to handle the case if size of the stats are more
     * than 512 bytes. Currently, the tx size is 34 bytes and ring buffer entry
     * size is 12 bytes.
     */
    u8 tx_stats[RING_BUF_ENTRY_SIZE];
};

typedef union {
    struct {
        u16 rate                            :  4;
        u16 nss                             :  2;
        u16 preamble                        :  2;
        u16 bw                              :  8;
    } mcs_s;
    u16 mcs;
} MCS;

typedef struct drv_msg_s
{
    u16 length;
    u16 event_type;
    u32 timestamp_low;
    u32 timestamp_high;
    union {
        struct {
            u32 version;
            u32 msg_seq_no;
            u32 payload_len;
            u8  payload[0];
        } __attribute__((packed)) pkt_stats_event;
    } u;
} __attribute__((packed)) drv_msg_t;

#endif
