
#ifndef BOOST_MPL_EMPTY_HPP_INCLUDED
#define BOOST_MPL_EMPTY_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2000-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: empty.hpp,v 1.2 2009-05-21 17:03:42 pvertenten Exp $
// $Date: 2009-05-21 17:03:42 $
// $Revision: 1.2 $

#include <boost/mpl/empty_fwd.hpp>
#include <boost/mpl/sequence_tag.hpp>
#include <boost/mpl/aux_/empty_impl.hpp>
#include <boost/mpl/aux_/na_spec.hpp>
#include <boost/mpl/aux_/lambda_support.hpp>

namespace boost { namespace mpl {

template<
      typename BOOST_MPL_AUX_NA_PARAM(Sequence)
    >
struct empty
    : empty_impl< typename sequence_tag<Sequence>::type >
        ::template apply< Sequence >
{
    BOOST_MPL_AUX_LAMBDA_SUPPORT(1,empty,(Sequence))
};

BOOST_MPL_AUX_NA_SPEC(1, empty)

}}

#endif // BOOST_MPL_EMPTY_HPP_INCLUDED
