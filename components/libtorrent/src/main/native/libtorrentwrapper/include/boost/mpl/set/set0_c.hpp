
#ifndef BOOST_MPL_SET_SET0_C_HPP_INCLUDED
#define BOOST_MPL_SET_SET0_C_HPP_INCLUDED

// Copyright Aleksey Gurtovoy 2003-2004
// Copyright David Abrahams 2003-2004
//
// Distributed under the Boost Software License, Version 1.0. 
// (See accompanying file LICENSE_1_0.txt or copy at 
// http://www.boost.org/LICENSE_1_0.txt)
//
// See http://www.boost.org/libs/mpl for documentation.

// $Id: set0_c.hpp,v 1.2 2009-05-21 17:03:49 pvertenten Exp $
// $Date: 2009-05-21 17:03:49 $
// $Revision: 1.2 $

#include <boost/mpl/set/set0.hpp>
#include <boost/mpl/integral_c.hpp>

namespace boost { namespace mpl {

template< typename T > struct set0_c
    : set0<>
{
    typedef set0_c type;
    typedef T value_type;
};

}}

#endif // BOOST_MPL_SET_SET0_C_HPP_INCLUDED
