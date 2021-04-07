//
// Created by YouXi on 2020/7/14.
//

#include "resample_helper.h"

void clear_context(SwrContextExt *s){
    swr_free(&s->swr_ctx);
    if (s->src_buffers) {
        av_freep(&s->src_buffers[0]);
    }
    av_freep(&s->src_buffers);

    if (s->dst_buffers) {
        av_freep(&s->dst_buffers[0]);
    }
    av_freep(&s->dst_buffers);
}

void swr_ext_free(SwrContextExt **ss) {
    SwrContextExt *s= *ss;
    if(s){
        clear_context(s);
    }
    av_freep(ss);
}

int convert_samples(int bytes_len, int bytes_per_sample, int nb_channels) {
    int nb_samples = bytes_len / bytes_per_sample / nb_channels;
    return nb_samples;
}
